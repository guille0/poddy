package com.guille.poddy.database;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import android.util.*;
import java.text.FieldPosition;
import com.guille.poddy.*;

public class RssParser {

    public static ParsedRss parseXMLFromFile(String rssFile, String url) {
        XmlPullParserFactory parserFactory;
        try {
            parserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserFactory.newPullParser();

            // Load the file into an inputstream
            File file = new File(rssFile);
            FileInputStream is = new FileInputStream(file);

            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(is, null);

            return processParsing(parser, url);

        } catch (Exception e) {
            // ERROR WHILE PARSING, return empty ParsedRss with null on both
            return new ParsedRss();
        }
    }

    public static ParsedRss parseXMLFromStream(InputStream is, String url) {
        XmlPullParserFactory parserFactory;
        try {
            parserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserFactory.newPullParser();

            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(is, null);


            return processParsing(parser, url);

        } catch (XmlPullParserException | IOException e) {
            // ERROR WHILE PARSING, return empty ParsedRss with null on both?
            return new ParsedRss();

        }
    }

    private static ParsedRss processParsing(XmlPullParser parser, String url)
            throws IOException, XmlPullParserException{

        List<Episode> episodes = new ArrayList<>();
        Podcast podcast = new Podcast();
        podcast.url = url;

        int eventType = parser.getEventType();
        Episode currentEp = null;

        boolean inChannel = false;
        boolean inImage = false;

        SimpleDateFormat extractDate = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);

        SimpleDateFormat convertDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        Date date = new Date();


        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tag;

            switch (eventType) {
                case XmlPullParser.END_TAG:
                    tag = parser.getName();
                    switch (tag) {
                        case "item":
                            if (currentEp.guid.equals("")) {
                                currentEp.guid = currentEp.enclosureUrl;
                            }
                            episodes.add(currentEp);
                            break;
                        case "channel":
                            inChannel = false;
                            break;
                        case "image":
                            inImage = false;
                    }


                case XmlPullParser.START_TAG:
                    tag = parser.getName();

                    // Podcast

                    switch (tag) {
                        case "channel":
                            inChannel = true;
                            break;
                        case "item":
                            inChannel = false;
                            inImage = false;
                            currentEp = new Episode();
                            break;
                    }

                    if (inChannel) {
                        switch (tag) {
                            case "title":
                                podcast.title = parser.nextText();
                                break;
                            case "description":
                                podcast.description = parser.nextText();
                                break;
                            case "language":
                                podcast.language = parser.nextText();
                                break;
                            case "author":
                                podcast.author= parser.nextText();
                                break;
                            case "link":
                                podcast.link= parser.nextText();
                                break;
                            case "image":
                                inImage = true;
                                break;
                            case "url":
                                if (inImage) podcast.imageUrl = parser.nextText();
                                break;
                        }

                    }
                    // Episodes

                    if (currentEp != null) {
                        switch (tag) {
                            case "title":
                                currentEp.title = parser.nextText();
                                break;
                            case "description":
                                currentEp.description = parser.nextText();
                                break;
                            case "author":
                                currentEp.author = parser.nextText();
                                break;
                            case "guid":
                                currentEp.guid = parser.nextText();
                                break;
                            case "image":
                                currentEp.imageUrl = parser.nextText();
                                break;
                            case "episode":
                                final String episode = parser.nextText();
                                if (episode.matches("^\\d+$"))
                                    currentEp.episode = Integer.parseInt(episode);
                                break;
                            case "duration":
                                final String duration = parser.nextText();
                                if (duration.matches("^[0-9][0-9]:[0-9][0-9]:[0-9][0-9]$"))
                                    currentEp.duration = Helpers.hhmmssToMs(duration);
                                else if (duration.matches("^\\d+$"))
                                    currentEp.duration = 1000*Long.parseLong(duration);
                                break;
                            case "pubDate":
                                // Convert date to ISO8601 for sqlite storage
                                try {
                                    date = extractDate.parse(parser.nextText());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    StringBuffer stringBuffer = new StringBuffer();
                                    convertDate.format(date, stringBuffer, new FieldPosition(0));
                                    currentEp.date = stringBuffer.toString();
                                }
                                break;
                            case "enclosure":
                                currentEp.enclosureUrl = parser.getAttributeValue(null, "url");
                                currentEp.enclosureType = parser.getAttributeValue(null, "type");
                                currentEp.enclosureLength = Integer.parseInt(parser.getAttributeValue(null, "length"));
                                break;
                        }
                    }
            }

            eventType = parser.next();
        }

        ParsedRss parsedRss = new ParsedRss();
        parsedRss.podcast = podcast;
        parsedRss.episodes = episodes;
        return parsedRss;
    }

}
