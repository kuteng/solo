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

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.b3log.latke.Keys;
import org.b3log.solo.model.Article;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

public class Utils {
    private static final String DEFAULT_TAG = "Note";

    public static void main(String[] args) {
        String fileContent = "---\n" +
                "id: adsfasd\n" +
                "title: 闫冬\n" +
                "date: 2020-01-29 01:02:40\n" +
                "---\n" +
                "# 闫冬\n" +
                "\n" +
                "我就是这么牛逼。";
        String frontMatter = StringUtils.substringBefore(fileContent, "---");
        System.out.println(">>> " + frontMatter);
        fileContent = StringUtils.substringAfter(fileContent, "---");
        frontMatter = StringUtils.substringBefore(fileContent, "---");
        System.out.println(">>> " + frontMatter);

        final Yaml yaml = new Yaml();
        Map elems;
        Object obj = yaml.load(frontMatter);

        if(obj instanceof  String) {
            elems = new HashMap();
            System.out.println(">>> elems: " + obj);
        }
        else {
            elems = (Map) yaml.load(frontMatter);
            System.out.println(">>> elems: " + JSON.toJSONString(elems));
        }
    }

    public static JSONObject parseArticle(final String fileName, String fileContent, Logger logger) {
        fileContent = StringUtils.trim(fileContent);
        String frontMatter = StringUtils.substringBefore(fileContent, "---");

        if (StringUtils.isBlank(frontMatter)) {
            fileContent = StringUtils.substringAfter(fileContent, "---");
            frontMatter = StringUtils.substringBefore(fileContent, "---");
        }

        final JSONObject ret = new JSONObject();
        final Yaml yaml = new Yaml();
        Map elems;

        try {
            elems = (Map) yaml.load(frontMatter);
        }
        catch (final Exception e) {
            // treat it as plain markdown
            logger.warn("treat it as plain markdown.", e);
            ret.put(Article.ARTICLE_TITLE, StringUtils.substringBeforeLast(fileName, "."));
            ret.put(Article.ARTICLE_CONTENT, fileContent);
            ret.put(Article.ARTICLE_ABSTRACT, Article.getAbstractText(fileContent));
            ret.put(Article.ARTICLE_TAGS_REF, DEFAULT_TAG);
            ret.put(Article.ARTICLE_STATUS, Article.ARTICLE_STATUS_C_PUBLISHED);
            ret.put(Article.ARTICLE_COMMENTABLE, true);
            ret.put(Article.ARTICLE_VIEW_PWD, "");

            return ret;
        }

        String id = parseId(elems, logger);

        if (!StringUtils.isBlank(id)) {
            ret.put(Keys.OBJECT_ID, id);
        }

        String title = (String) elems.get("title");
        if (StringUtils.isBlank(title)) {
            title = StringUtils.substringBeforeLast(fileName, ".");
        }
        ret.put(Article.ARTICLE_TITLE, title);

        String content = StringUtils.substringAfter(fileContent, frontMatter);
        if (StringUtils.startsWith(content, "---")) {
            content = StringUtils.substringAfter(content, "---");
            content = StringUtils.trim(content);
        }
        ret.put(Article.ARTICLE_CONTENT, content);

        final String abs = parseAbstract(elems, content, logger);
        ret.put(Article.ARTICLE_ABSTRACT, abs);

        final Date date = parseDate(elems, logger);
        ret.put(Article.ARTICLE_CREATED, date.getTime());

        final String permalink = (String) elems.get("permalink");
        if (StringUtils.isNotBlank(permalink)) {
            ret.put(Article.ARTICLE_PERMALINK, permalink);
        }

        final List<String> tags = parseTags(elems, logger);
        final StringBuilder tagBuilder = new StringBuilder();
        for (final String tag : tags) {
            tagBuilder.append(tag).append(",");
        }
        tagBuilder.deleteCharAt(tagBuilder.length() - 1);
        ret.put(Article.ARTICLE_TAGS_REF, tagBuilder.toString());

        ret.put(Article.ARTICLE_STATUS, Article.ARTICLE_STATUS_C_PUBLISHED);
        ret.put(Article.ARTICLE_COMMENTABLE, true);
        ret.put(Article.ARTICLE_VIEW_PWD, parseViewPwd(elems, logger));

        return ret;
    }

    /**
     * 解析文件头中的 id 字段。
     * @param map
     * @param logger
     * @return
     */
    private static String parseId(final Map map, Logger logger) {
        String id = "";

        if(null == map.get("id")) {
            id = null;
        }
        else if(map.get("id") instanceof Integer) {
            id = (Integer) map.get("id") + "";
        }
        else {
            id = (String) map.get("id");
        }

        return id;
    }

    /**
     * 解析文件头中的 pwd 或 password 字段。
     * @param map
     * @param logger
     * @return
     */
    private static String parseViewPwd(final Map map, Logger logger) {
        String ret = null == map.get("pwd") ? null : map.get("pwd").toString();

        if(StringUtils.isNotEmpty(ret)) {
            return ret;
        }

        ret = null == map.get("password") ? null : map.get("password").toString();

        if(StringUtils.isNotEmpty(ret)) {
            return ret;
        }

        return "";
    }

    private static String parseAbstract(final Map map, final String content, Logger logger) {
        String ret = (String) map.get("description");
        if (null == ret) {
            ret = (String) map.get("summary");
        }
        if (null == ret) {
            ret = (String) map.get("abstract");
        }
        if (StringUtils.isNotBlank(ret)) {
            return ret;
        }

        if(StringUtils.isEmpty((String) map.get("pwd"))) {
            return Article.getAbstractText(content);
        }

        return "";
    }

    private static Date parseDate(final Map map, Logger logger) {
        Object date = map.get("date");
        if (null == date) {
            return new Date();
        }

        if (date instanceof String) {
            try {
                return DateUtils.parseDate((String) date, new String[]{
                        "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "dd/MM/yyyy HH:mm:ss",
                        "dd-MM-yyyy HH:mm:ss", "yyyyMMdd HH:mm:ss",
                        "yyyy/MM/dd HH:mm", "yyyy-MM-dd HH:mm", "dd/MM/yyyy HH:mm",
                        "dd-MM-yyyy HH:mm", "yyyyMMdd HH:mm"});
            } catch (final Exception e) {
                logger.log(Level.ERROR, "Parse date [" + date + "] failed", e);

                throw new RuntimeException(e);
            }
        } else if (date instanceof Date) {
            return (Date) date;
        }

        return new Date();
    }

    private static List<String> parseTags(final Map map, Logger logger) {
        final List<String> ret = new ArrayList<>();

        Object tags = map.get("tags");
        if (null == tags) {
            tags = map.get("category");
        }
        if (null == tags) {
            tags = map.get("categories");
        }
        if (null == tags) {
            tags = map.get("keyword");
        }
        if (null == tags) {
            tags = map.get("keywords");
        }
        if (null == tags) {
            ret.add(DEFAULT_TAG);

            return ret;
        }

        if (tags instanceof String) {
            final String[] tagArr = ((String) tags).split(" ");
            tags = Arrays.asList(tagArr);
        }
        final TreeSet tagSet = new TreeSet();
        for (final String tag : (List<String>) tags) {
            if (StringUtils.isBlank(tag)) {
                tagSet.add(DEFAULT_TAG);
            } else {
                tagSet.add(tag);
            }
        }
        ret.addAll(tagSet);

        return ret;
    }
}
