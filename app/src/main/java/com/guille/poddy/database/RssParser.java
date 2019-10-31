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
import java.text.FieldPosition;

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

        SimpleDateFormat extractDate = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");

        SimpleDateFormat convertDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date date = new Date();


        while (eventType != XmlPullParser.END_DOCUMENT) {
            String eltName;

            switch (eventType) {
                case XmlPullParser.END_TAG:
                    eltName = parser.getName();
                    if ("item".equals(eltName)) {
                        episodes.add(currentEp);
                    } else if ("channel".equals(eltName)) {
                        inChannel = false;
                    } else if ("image".equals(eltName)) {
                        inImage = false;
                    }


                case XmlPullParser.START_TAG:
                    eltName = parser.getName();

                    // Podcast

                    if ("channel".equals(eltName)) {
                        inChannel = true;
                    }
                    if (inChannel){
                        if ("title".equals(eltName)) {
                            podcast.title = parser.nextText();
                        } else if ("description".equals(eltName)) {
                            podcast.description = parser.nextText();
                        } else if ("image".equals(eltName)) {
                            inImage = true;
                        }
                        if (inImage && "url".equals(eltName)) {
                            podcast.imageUrl = parser.nextText();
                        }

                    }
                        // Episodes

                    if ("item".equals(eltName)) {
                        inChannel = false;
                        inImage = false;
                        currentEp = new Episode();
                    } else if (currentEp != null) {
                        if ("title".equals(eltName)) {
                            currentEp.title = parser.nextText();
                        } else if ("description".equals(eltName)) {
                            currentEp.description = parser.nextText();
                        } else if ("pubDate".equals(eltName)) {
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

                        } else if (eltName.equals("enclosure")) {
                            currentEp.enclosureUrl = parser.getAttributeValue(null, "url");
                            currentEp.enclosureType = parser.getAttributeValue(null, "type");
                            currentEp.enclosureLength = Integer.parseInt(parser.getAttributeValue(null, "length"));
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
