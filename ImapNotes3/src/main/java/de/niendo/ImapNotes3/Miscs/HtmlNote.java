/*
 * Copyright (C) 2022-2023 - Peter Korf <peter@niendo.de>
 * and Contributors.
 *
 * This file is part of ImapNotes3.
 *
 * ImapNotes3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.niendo.ImapNotes3.Miscs;

import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;

import androidx.annotation.NonNull;

import de.niendo.ImapNotes3.Data.OneNote;
import de.niendo.ImapNotes3.ImapNotes3;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


public class HtmlNote {

    private static final String TAG = "IN_HtmlNote";
    private static final Pattern patternBodyBgColor = Pattern.compile("background-color:(.*?);", Pattern.MULTILINE);

    public final String text;
    @NonNull
    public final String color;

    public HtmlNote(String text,
                    @NonNull String color) {
        this.text = text;
        this.color = color;
    }

    @NonNull
    public static Message GetMessageFromNote(@NonNull OneNote note, String noteBody) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage message = new MimeMessage(session);

        message.setHeader("X-Uniform-Type-Identifier", "com.apple.mail-note");
        UUID uuid = UUID.randomUUID();
        message.setHeader("X-Universally-Unique-Identifier", uuid.toString());
        message.setHeader("X-Mailer", Utilities.FullApplicationName);

/*
            <!DOCTYPE html>
            <html>
            <body style="background-color:khaki;">
            </body>
            </html>
*/
        Document doc = Jsoup.parse(noteBody, "utf-8");
        String bodyStyle = doc.select("body").attr("style");
        doc.outputSettings().prettyPrint(false);
        if (!note.GetBgColor().equals("none")) {
            Matcher matcherColor = HtmlNote.patternBodyBgColor.matcher(bodyStyle);
            String BgColorStr = "background-color:" + note.GetBgColor() + ";";
            if (matcherColor.find()) {
                bodyStyle = matcherColor.replaceFirst(BgColorStr);
            } else {
                bodyStyle = BgColorStr + bodyStyle;
            }

            doc.select("body").attr("style", bodyStyle);
        }
/*          body = body.replaceFirst("<p dir=ltr>", "<div>");
            body = body.replaceFirst("<p dir=\"ltr\">", "<div>");
            body = body.replaceAll("<p dir=ltr>", "<div><br></div><div>");
            body = body.replaceAll("<p dir=\"ltr\">", "<div><br></div><div>");
            body = body.replaceAll("</p>", "</div>");
            body = body.replaceAll("<br>\n", "</div><div>");
 */

        message.setText(doc.toString(), "utf-8", "html");
        message.setFlag(Flags.Flag.SEEN, true);

        return (message);
    }

    @NonNull
    public static HtmlNote GetNoteFromMessage(@NonNull Message message) {
        ContentType contentType = null;
        String stringres = "";
        //InputStream iis = null;
        //String charset;

        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        CommandMap.setDefaultCommandMap(mc);


        try {
            Log.d(TAG, "GetNoteFromMessage :" + message);
            contentType = new ContentType(message.getContentType());

            if (message.isMimeType("multipart/*")) {
                MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
                stringres = getTextFromMimeMultipart(mimeMultipart);
            } else {
                if (contentType.match("text/html")) {
                    stringres = (String) message.getContent();
                }
                // import plain text notes
                if (contentType.match("text/plain")) {
                    stringres = (String) message.getContent();
                    Spannable text = new SpannableString(stringres);
                    stringres = Html.toHtml(text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
                    stringres = stringres.replaceFirst("<p dir=\"ltr\">", "");
                }
            }

        } catch (Exception e) {
            Log.d(TAG, "Exception GetNoteFromMessage:" + e.toString());
            e.printStackTrace();
        }

        return new HtmlNote(
                getText(stringres),
                getColor(stringres));
    }

    // https://stackoverflow.com/questions/11240368/how-to-read-text-inside-body-of-mail-using-javax-mail
    private static String getTextFromMimeMultipart(
            MimeMultipart mimeMultipart) throws IOException, MessagingException {

        int count = mimeMultipart.getCount();
        if (count == 0)
            throw new MessagingException("Multipart with no body parts not supported.");
        boolean multipartAlt = new ContentType(mimeMultipart.getContentType()).match("multipart/alternative");
        if (multipartAlt)
            // alternatives appear in an order of increasing
            // faithfulness to the original content. Customize as req'd.
            return getTextFromBodyPart(mimeMultipart.getBodyPart(count - 1));
        String result = "";
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            result += getTextFromBodyPart(bodyPart);
        }
        return result;
    }

    private static String getTextFromBodyPart(
            BodyPart bodyPart) throws IOException, MessagingException {

        String result = "";
        if (bodyPart.isMimeType("text/plain")) {
            Spannable text = new SpannableString((String) bodyPart.getContent());
            result = Html.toHtml(text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
            result = result.replaceFirst("<p dir=\"ltr\">", "");
        } else if (bodyPart.isMimeType("text/html")) {
            result = (String) bodyPart.getContent();
            //result = org.jsoup.Jsoup.parse(html).text();
        } else if (bodyPart.isMimeType("image/*")) {
            MimeBodyPart mp = (MimeBodyPart) bodyPart;
            //mp.saveFile(ImapNotes3.GetRootDir()+mp.getFileName());
        } else if (bodyPart.getContent() instanceof MimeMultipart) {
            result = getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
        }
        return result;
    }

    private static String getText(@NonNull String stringres) {
        return stringres;
    }

    @NonNull
    private static String getColor(@NonNull String stringres) {
        Document doc = Jsoup.parse(stringres, "utf-8");
        String bodyStyle = doc.select("body").attr("style");
        Matcher matcherColor = patternBodyBgColor.matcher(bodyStyle);
        if (matcherColor.find()) {
            String colorName = matcherColor.group(1).toLowerCase(Locale.ROOT);
            return ((colorName.isEmpty()) || colorName.equals("null") || colorName.equals("transparent")) ? "none" : colorName;
        } else {
            return "none";
        }
    }

}
