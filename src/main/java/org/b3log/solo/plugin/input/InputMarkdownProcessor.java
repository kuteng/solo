/*
 * Solo - A small and beautiful blogging system written in Java.
 * Copyright (c) 2010-present, b3log.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.b3log.solo.plugin.input;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.b3log.latke.Keys;
import org.b3log.latke.http.FileUpload;
import org.b3log.latke.http.HttpMethod;
import org.b3log.latke.http.RequestContext;
import org.b3log.latke.http.annotation.RequestProcessing;
import org.b3log.latke.http.annotation.RequestProcessor;
import org.b3log.latke.ioc.Inject;
import org.b3log.latke.service.ServiceException;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Common;
import org.b3log.solo.processor.ArticleProcessor;
import org.b3log.solo.service.ArticleMgmtService;
import org.b3log.solo.service.ArticleQueryService;
import org.b3log.solo.service.UserQueryService;
import org.b3log.solo.util.Markdowns;
import org.b3log.solo.util.Solos;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 引入Markdown文档
 */
@RequestProcessor
public class InputMarkdownProcessor {
    /**
     * Logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(InputMarkdownProcessor.class);

    /**
     * Article management service.
     */
    @Inject
    private ArticleMgmtService articleMgmtService;

    /**
     * Article query service.
     */
    @Inject
    private ArticleQueryService articleQueryService;

    /**
     * User query service.
     */
    @Inject
    private UserQueryService userQueryService;

    /**
     * 引入文档
     * @param context
     */
    // @RequestProcessing(value = "/plugin/markdown/input", method = HttpMethod.POST)
    @RequestProcessing(value = "/plugin/markdown/input", method = {HttpMethod.GET, HttpMethod.POST})
    public void markdown2HTML(final RequestContext context) {
        LOGGER.info(">>> 请求已接收。");

        final JSONObject result = Solos.newSucc();
        context.renderJSON(result);
        JSONObject obj = context.requestJSON();
        FileUpload fileUpload = context.getRequest().getFileUpload("file");
        byte[] contentBytes = fileUpload.getData();

        // final String markdownText = context.requestJSON().optString("markdownText");
        // if (StringUtils.isBlank(markdownText)) {
        // }

        try {
            String content = new String(contentBytes, "utf-8");
            String id = parseArticle("", content);
            JSONObject res = new JSONObject();
            res.put("id", id);
            result.put(Keys.MSG, "成功了");
            result.put(Common.DATA, res);
        }
        catch (final UnsupportedEncodingException e) {
            LOGGER.log(Level.ERROR, e.getMessage(), e);
            result.put(Keys.CODE, -1);
            result.put(Keys.MSG, "文件内容解析出错，请确认是UTF-8编码。");
        }
        catch (final Exception e) {
            LOGGER.log(Level.ERROR, e.getMessage(), e);
            result.put(Keys.CODE, -1);
            result.put(Keys.MSG, "出错了");
        }
    }

    private String parseArticle(String fileName, String fileContent) throws ServiceException {
        final JSONObject admin = userQueryService.getAdmin();

        if (null == admin) { // Not init yet
            return null;
        }

        final String adminId = admin.optString(Keys.OBJECT_ID);
        JSONObject article = Utils.parseArticle(fileName, fileContent, LOGGER);

        // LOGGER.info(">>> getID: " + String.valueOf(article.getString(Keys.OBJECT_ID)) + "; optID: " + String.valueOf(article.optString(Keys.OBJECT_ID)));
        LOGGER.info(">>> optID: " + String.valueOf(article.optString(Keys.OBJECT_ID)));

        article.put(Article.ARTICLE_AUTHOR_ID, adminId);
        final JSONObject request = new JSONObject();
        request.put(Article.ARTICLE, article);
        String fileArticleId = article.optString(Keys.OBJECT_ID);
        String id = null;

        if(null == articleQueryService.getArticle(fileArticleId)) {
            id = articleMgmtService.addArticle(request);
        }
        else {
            articleMgmtService.updateArticle(request);
            id = fileArticleId;
        }


        LOGGER.info("<<< getID: " + String.valueOf(article.getString(Keys.OBJECT_ID)) + "; optID: " + String.valueOf(article.optString(Keys.OBJECT_ID)));

        LOGGER.info("Imported article [" + article.optString(Article.ARTICLE_TITLE) + "]");
        return id;
    }
}
