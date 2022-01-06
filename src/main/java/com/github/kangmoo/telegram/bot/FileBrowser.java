package com.github.kangmoo.telegram.bot;

import org.slf4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author kangmoo Heo
 */
public class FileBrowser {
    private static final Logger logger = getLogger(FileBrowser.class);

    public void start(int port, String wwwHome) {
        // read arguments
        String wwwhome = wwwHome;

        // open server socket
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(port);
        } catch (IOException e) {
            logger.warn("Could not start server", e);
            System.exit(-1);
        }
        logger.debug("FileServer accepting connections on port {}", port);

        // request handler loop
        while (true) {
            Socket connection = null;
            try {
                // wait for request
                connection = socket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                OutputStream out = new BufferedOutputStream(connection.getOutputStream());
                PrintStream pout = new PrintStream(out);

                // read first line of request (ignore the rest)
                String request = in.readLine();
                if (request == null)
                    continue;
                log(connection, request);
                while (true) {
                    String misc = in.readLine();
                    if (misc == null || misc.length() == 0)
                        break;
                }

                // parse the line
                if (!request.startsWith("GET") || request.length() < 14 ||
                        !(request.endsWith("HTTP/1.0") || request.endsWith("HTTP/1.1"))) {
                    // bad request
                    errorReport(pout, connection, "400", "Bad Request",
                            "Your browser sent a request that " +
                                    "this server could not understand.");
                } else {
                    String req = request.substring(4, request.length() - 9).trim();
                    if (req.indexOf("..") != -1 ||
                            req.indexOf("/.ht") != -1 || req.endsWith("~")) {
                        // evil hacker trying to read non-wwwhome or secret file
                        errorReport(pout, connection, "403", "Forbidden",
                                "You don't have permission to access the requested URL.");
                    } else {
                        String path = wwwhome + "/" + req;
                        File f = new File(path);
                        if (f.isDirectory() && !path.endsWith("/")) {
                            // redirect browser if referring to directory without final '/'
                            pout.print("HTTP/1.0 301 Moved Permanently\r\n" +
                                    "Location: http://" +
                                    connection.getLocalAddress().getHostAddress() + ":" +
                                    connection.getLocalPort() + "/" + req + "/\r\n\r\n");
                            log(connection, "301 Moved Permanently");
                        } else {
                            if (f.isDirectory()) {
                                // if directory, implicitly add 'index.html'
                                // path = path + "index.html";
                                pout.print("HTTP/1.0 200 OK\r\n" +
                                        "Content-Type: text/html\r\n" +
                                        "Date: " + new Date() + "\r\n" +
                                        "Server: FileServer 1.0\r\n\r\n");
                                sendData(makeIndexHtml(new File(path), wwwhome), out);
                                log(connection, "200 OK");
                                continue;
                            } else {
                                try (InputStream file = new FileInputStream(f)) {
                                    // send file
                                    pout.print("HTTP/1.0 200 OK\r\n" +
                                            "Content-Type: " + guessContentType(path) + "\r\n" +
                                            "Date: " + new Date() + "\r\n" +
                                            "Server: FileServer 1.0\r\n\r\n");
                                    sendFile(file, out); // send raw file
                                    log(connection, "200 OK");
                                } catch (FileNotFoundException e) {
                                    // file not found
                                    errorReport(pout, connection, "404", "Not Found",
                                            "The requested URL was not found on this server.");
                                }
                            }

                        }
                    }
                }
                out.flush();
            } catch (IOException e) {
                logger.warn("Err Occurs", e);
            }
            try {
                if (connection != null) connection.close();
            } catch (IOException e) {
                logger.warn("ERR Occurs", e);
            }
        }
    }

    private static void log(Socket connection, String msg) {
        logger.warn(new Date() + " [" + connection.getInetAddress().getHostAddress() +
                ":" + connection.getPort() + "] " + msg);
    }

    private static void errorReport(PrintStream pout, Socket connection,
                                    String code, String title, String msg) {
        pout.print("HTTP/1.0 " + code + " " + title + "\r\n" +
                "\r\n" +
                "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\r\n" +
                "<TITLE>" + code + " " + title + "</TITLE>\r\n" +
                "</HEAD><BODY>\r\n" +
                "<H1>" + title + "</H1>\r\n" + msg + "<P>\r\n" +
                "<HR><ADDRESS>FileServer 1.0 at " +
                connection.getLocalAddress().getHostName() +
                " Port " + connection.getLocalPort() + "</ADDRESS>\r\n" +
                "</BODY></HTML>\r\n");
        log(connection, code + " " + title);
    }

    private static String guessContentType(String path) {
        if (path.endsWith(".html") || path.endsWith(".htm"))
            return "text/html";
        else if (path.endsWith(".txt") || path.endsWith(".java"))
            return "text/plain";
        else if (path.endsWith(".gif"))
            return "image/gif";
        else if (path.endsWith(".class"))
            return "application/octet-stream";
        else if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
            return "image/jpeg";
        else
            return "text/plain";
    }

    private static void sendFile(InputStream file, OutputStream out) {
        try {
            byte[] buffer = new byte[1000];
            while (file.available() > 0)
                out.write(buffer, 0, file.read(buffer));
        } catch (IOException e) {
            logger.warn("Err Occurs while sending file", e);
        }
    }

    private static void sendData(String msg, OutputStream out) {
        try {
            out.write(msg.getBytes());
        } catch (IOException e) {
            logger.warn("Err Occurs while sending date", e);
        }
    }


    public static String makeIndexHtml(File file, String wwwhome) {
        StringBuilder sb = new StringBuilder();
        sb.append(indexTemplate.replaceAll("\\$FILE", file.getAbsolutePath().replace(wwwhome, "")));
        Optional.ofNullable(file.listFiles()).ifPresent(list -> {
            for (File f : list) {
                sb.append("<script>addRow(")
                        .append(String.format("\"%s\",\"%s\",%d,%d,\"%s\",%d,\"%s\"", f.getName(), f.getName(), f.isDirectory() ? 1 : 0, f.length(),
                                readableFileSize(f.length()), f.lastModified(),
                                new SimpleDateFormat("yy.M.D. a HH:mm:ss").format(new Date(f.lastModified()))))
                        .append(");</script>\n");
            }
        });

        sb.append("</body></html>");
        return sb.toString();
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }


    private static final String indexTemplate = "<!DOCTYPE html>\n" +
            "<html dir=\"ltr\" lang=\"ko\"><head>\n" +
            "<meta charset=\"utf-8\">\n" +
            "<meta name=\"google\" value=\"notranslate\">\n" +
            "\n" +
            "<script>\n" +
            "function addRow(name, url, isdir,\n" +
            "    size, size_string, date_modified, date_modified_string) {\n" +
            "  if (name == \".\" || name == \"..\")\n" +
            "    return;\n" +
            "\n" +
            "  var root = document.location.pathname;\n" +
            "  if (root.substr(-1) !== \"/\")\n" +
            "    root += \"/\";\n" +
            "\n" +
            "  var tbody = document.getElementById(\"tbody\");\n" +
            "  var row = document.createElement(\"tr\");\n" +
            "  var file_cell = document.createElement(\"td\");\n" +
            "  var link = document.createElement(\"a\");\n" +
            "\n" +
            "  link.className = isdir ? \"icon dir\" : \"icon file\";\n" +
            "\n" +
            "  if (isdir) {\n" +
            "    name = name + \"/\";\n" +
            "    url = url + \"/\";\n" +
            "    size = 0;\n" +
            "    size_string = \"\";\n" +
            "  } else {\n" +
            "    link.draggable = \"true\";\n" +
            "    link.addEventListener(\"dragstart\", onDragStart, false);\n" +
            "  }\n" +
            "  link.innerText = name;\n" +
            "  link.href = root + url;\n" +
            "\n" +
            "  file_cell.dataset.value = name;\n" +
            "  file_cell.appendChild(link);\n" +
            "\n" +
            "  row.appendChild(file_cell);\n" +
            "  row.appendChild(createCell(size, size_string));\n" +
            "  row.appendChild(createCell(date_modified, date_modified_string));\n" +
            "\n" +
            "  tbody.appendChild(row);\n" +
            "}\n" +
            "\n" +
            "function onDragStart(e) {\n" +
            "  var el = e.srcElement;\n" +
            "  var name = el.innerText.replace(\":\", \"\");\n" +
            "  var download_url_data = \"application/octet-stream:\" + name + \":\" + el.href;\n" +
            "  e.dataTransfer.setData(\"DownloadURL\", download_url_data);\n" +
            "  e.dataTransfer.effectAllowed = \"copy\";\n" +
            "}\n" +
            "\n" +
            "function createCell(value, text) {\n" +
            "  var cell = document.createElement(\"td\");\n" +
            "  cell.setAttribute(\"class\", \"detailsColumn\");\n" +
            "  cell.dataset.value = value;\n" +
            "  cell.innerText = text;\n" +
            "  return cell;\n" +
            "}\n" +
            "\n" +
            "function start(location) {\n" +
            "  var header = document.getElementById(\"header\");\n" +
            "  header.innerText = header.innerText.replace(\"LOCATION\", location);\n" +
            "\n" +
            "  document.getElementById(\"title\").innerText = header.innerText;\n" +
            "}\n" +
            "\n" +
            "function onHasParentDirectory() {\n" +
            "  var box = document.getElementById(\"parentDirLinkBox\");\n" +
            "  box.style.display = \"block\";\n" +
            "\n" +
            "  var root = document.location.pathname;\n" +
            "  if (!root.endsWith(\"/\"))\n" +
            "    root += \"/\";\n" +
            "\n" +
            "  var link = document.getElementById(\"parentDirLink\");\n" +
            "  link.href = root + \"..\";\n" +
            "}\n" +
            "\n" +
            "function onListingParsingError() {\n" +
            "  var box = document.getElementById(\"listingParsingErrorBox\");\n" +
            "  box.innerHTML = box.innerHTML.replace(\"LOCATION\", encodeURI(document.location)\n" +
            "      + \"?raw\");\n" +
            "  box.style.display = \"block\";\n" +
            "}\n" +
            "\n" +
            "function sortTable(column) {\n" +
            "  var theader = document.getElementById(\"theader\");\n" +
            "  var oldOrder = theader.cells[column].dataset.order || '1';\n" +
            "  oldOrder = parseInt(oldOrder, 10)\n" +
            "  var newOrder = 0 - oldOrder;\n" +
            "  theader.cells[column].dataset.order = newOrder;\n" +
            "\n" +
            "  var tbody = document.getElementById(\"tbody\");\n" +
            "  var rows = tbody.rows;\n" +
            "  var list = [], i;\n" +
            "  for (i = 0; i < rows.length; i++) {\n" +
            "    list.push(rows[i]);\n" +
            "  }\n" +
            "\n" +
            "  list.sort(function(row1, row2) {\n" +
            "    var a = row1.cells[column].dataset.value;\n" +
            "    var b = row2.cells[column].dataset.value;\n" +
            "    if (column) {\n" +
            "      a = parseInt(a, 10);\n" +
            "      b = parseInt(b, 10);\n" +
            "      return a > b ? newOrder : a < b ? oldOrder : 0;\n" +
            "    }\n" +
            "\n" +
            "    // Column 0 is text.\n" +
            "    if (a > b)\n" +
            "      return newOrder;\n" +
            "    if (a < b)\n" +
            "      return oldOrder;\n" +
            "    return 0;\n" +
            "  });\n" +
            "\n" +
            "  // Appending an existing child again just moves it.\n" +
            "  for (i = 0; i < list.length; i++) {\n" +
            "    tbody.appendChild(list[i]);\n" +
            "  }\n" +
            "}\n" +
            "\n" +
            "// Add event handlers to column headers.\n" +
            "function addHandlers(element, column) {\n" +
            "  element.onclick = (e) => sortTable(column);\n" +
            "  element.onkeydown = (e) => {\n" +
            "    if (e.key == 'Enter' || e.key == ' ') {\n" +
            "      sortTable(column);\n" +
            "      e.preventDefault();\n" +
            "    }\n" +
            "  };\n" +
            "}\n" +
            "\n" +
            "function onLoad() {\n" +
            "  addHandlers(document.getElementById('nameColumnHeader'), 0);\n" +
            "  addHandlers(document.getElementById('sizeColumnHeader'), 1);\n" +
            "  addHandlers(document.getElementById('dateColumnHeader'), 2);\n" +
            "}\n" +
            "\n" +
            "window.addEventListener('DOMContentLoaded', onLoad);\n" +
            "</script>\n" +
            "\n" +
            "<style>\n" +
            "\n" +
            "  h1 {\n" +
            "    border-bottom: 1px solid #c0c0c0;\n" +
            "    margin-bottom: 10px;\n" +
            "    padding-bottom: 10px;\n" +
            "    white-space: nowrap;\n" +
            "  }\n" +
            "\n" +
            "  table {\n" +
            "    border-collapse: collapse;\n" +
            "  }\n" +
            "\n" +
            "  th {\n" +
            "    cursor: pointer;\n" +
            "  }\n" +
            "\n" +
            "  td.detailsColumn {\n" +
            "    padding-inline-start: 2em;\n" +
            "    text-align: end;\n" +
            "    white-space: nowrap;\n" +
            "  }\n" +
            "\n" +
            "  a.icon {\n" +
            "    padding-inline-start: 1.5em;\n" +
            "    text-decoration: none;\n" +
            "    user-select: auto;\n" +
            "  }\n" +
            "\n" +
            "  a.icon:hover {\n" +
            "    text-decoration: underline;\n" +
            "  }\n" +
            "\n" +
            "  a.file {\n" +
            "    background : url(\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAIAAACQkWg2AAAABnRSTlMAAAAAAABupgeRAAABHUlEQVR42o2RMW7DIBiF3498iHRJD5JKHurL+CRVBp+i2T16tTynF2gO0KSb5ZrBBl4HHDBuK/WXACH4eO9/CAAAbdvijzLGNE1TVZXfZuHg6XCAQESAZXbOKaXO57eiKG6ft9PrKQIkCQqFoIiQFBGlFIB5nvM8t9aOX2Nd18oDzjnPgCDpn/BH4zh2XZdlWVmWiUK4IgCBoFMUz9eP6zRN75cLgEQhcmTQIbl72O0f9865qLAAsURAAgKBJKEtgLXWvyjLuFsThCSstb8rBCaAQhDYWgIZ7myM+TUBjDHrHlZcbMYYk34cN0YSLcgS+wL0fe9TXDMbY33fR2AYBvyQ8L0Gk8MwREBrTfKe4TpTzwhArXWi8HI84h/1DfwI5mhxJamFAAAAAElFTkSuQmCC \") left top no-repeat;\n" +
            "  }\n" +
            "\n" +
            "  a.dir {\n" +
            "    background : url(\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAd5JREFUeNqMU79rFUEQ/vbuodFEEkzAImBpkUabFP4ldpaJhZXYm/RiZWsv/hkWFglBUyTIgyAIIfgIRjHv3r39MePM7N3LcbxAFvZ2b2bn22/mm3XMjF+HL3YW7q28YSIw8mBKoBihhhgCsoORot9d3/ywg3YowMXwNde/PzGnk2vn6PitrT+/PGeNaecg4+qNY3D43vy16A5wDDd4Aqg/ngmrjl/GoN0U5V1QquHQG3q+TPDVhVwyBffcmQGJmSVfyZk7R3SngI4JKfwDJ2+05zIg8gbiereTZRHhJ5KCMOwDFLjhoBTn2g0ghagfKeIYJDPFyibJVBtTREwq60SpYvh5++PpwatHsxSm9QRLSQpEVSd7/TYJUb49TX7gztpjjEffnoVw66+Ytovs14Yp7HaKmUXeX9rKUoMoLNW3srqI5fWn8JejrVkK0QcrkFLOgS39yoKUQe292WJ1guUHG8K2o8K00oO1BTvXoW4yasclUTgZYJY9aFNfAThX5CZRmczAV52oAPoupHhWRIUUAOoyUIlYVaAa/VbLbyiZUiyFbjQFNwiZQSGl4IDy9sO5Wrty0QLKhdZPxmgGcDo8ejn+c/6eiK9poz15Kw7Dr/vN/z6W7q++091/AQYA5mZ8GYJ9K0AAAAAASUVORK5CYII= \") left top no-repeat;\n" +
            "  }\n" +
            "\n" +
            "  a.up {\n" +
            "    background : url(\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAmlJREFUeNpsU0toU0EUPfPysx/tTxuDH9SCWhUDooIbd7oRUUTMouqi2iIoCO6lceHWhegy4EJFinWjrlQUpVm0IIoFpVDEIthm0dpikpf3ZuZ6Z94nrXhhMjM3c8895977BBHB2PznK8WPtDgyWH5q77cPH8PpdXuhpQT4ifR9u5sfJb1bmw6VivahATDrxcRZ2njfoaMv+2j7mLDn93MPiNRMvGbL18L9IpF8h9/TN+EYkMffSiOXJ5+hkD+PdqcLpICWHOHc2CC+LEyA/K+cKQMnlQHJX8wqYG3MAJy88Wa4OLDvEqAEOpJd0LxHIMdHBziowSwVlF8D6QaicK01krw/JynwcKoEwZczewroTvZirlKJs5CqQ5CG8pb57FnJUA0LYCXMX5fibd+p8LWDDemcPZbzQyjvH+Ki1TlIciElA7ghwLKV4kRZstt2sANWRjYTAGzuP2hXZFpJ/GsxgGJ0ox1aoFWsDXyyxqCs26+ydmagFN/rRjymJ1898bzGzmQE0HCZpmk5A0RFIv8Pn0WYPsiu6t/Rsj6PauVTwffTSzGAGZhUG2F06hEc9ibS7OPMNp6ErYFlKavo7MkhmTqCxZ/jwzGA9Hx82H2BZSw1NTN9Gx8ycHkajU/7M+jInsDC7DiaEmo1bNl1AMr9ASFgqVu9MCTIzoGUimXVAnnaN0PdBBDCCYbEtMk6wkpQwIG0sn0PQIUF4GsTwLSIFKNqF6DVrQq+IWVrQDxAYQC/1SsYOI4pOxKZrfifiUSbDUisif7XlpGIPufXd/uvdvZm760M0no1FZcnrzUdjw7au3vu/BVgAFLXeuTxhTXVAAAAAElFTkSuQmCC \") left top no-repeat;\n" +
            "  }\n" +
            "\n" +
            "  html[dir=rtl] a {\n" +
            "    background-position-x: right;\n" +
            "  }\n" +
            "\n" +
            "  #parentDirLinkBox {\n" +
            "    margin-bottom: 10px;\n" +
            "    padding-bottom: 10px;\n" +
            "  }\n" +
            "\n" +
            "  #listingParsingErrorBox {\n" +
            "    border: 1px solid black;\n" +
            "    background: #fae691;\n" +
            "    padding: 10px;\n" +
            "    display: none;\n" +
            "  }\n" +
            "</style>\n" +
            "\n" +
            "<title id=\"title\">$FILE/의 색인</title>\n" +
            "\n" +
            "</head>\n" +
            "\n" +
            "<body>\n" +
            "\n" +
            "<div id=\"listingParsingErrorBox\">이 서버가 Chrome에서 이해할 수 없는 데이터를 전송합니다. <a href=\"http://code.google.com/p/chromium/issues/entry\">버그를 신고</a>하고 <a href=\"LOCATION\">원본 목록</a>을 포함하세요.</div>\n" +
            "\n" +
            "<h1 id=\"header\">$FILE/의 색인</h1>\n" +
            "\n" +
            "<div id=\"parentDirLinkBox\" style=\"display: block;\">\n" +
            "  <a id=\"parentDirLink\" class=\"icon up\" href=\"$FILE/..\">\n" +
            "    <span id=\"parentDirText\">[상위 디렉터리]</span>\n" +
            "  </a>\n" +
            "</div>\n" +
            "\n" +
            "<table>\n" +
            "  <thead>\n" +
            "    <tr class=\"header\" id=\"theader\">\n" +
            "      <th id=\"nameColumnHeader\" tabindex=\"0\" role=\"button\">이름</th>\n" +
            "      <th id=\"sizeColumnHeader\" class=\"detailsColumn\" tabindex=\"0\" role=\"button\">\n" +
            "        크기\n" +
            "      </th>\n" +
            "      <th id=\"dateColumnHeader\" class=\"detailsColumn\" tabindex=\"0\" role=\"button\">\n" +
            "        수정된 날짜\n" +
            "      </th>\n" +
            "    </tr>\n" +
            "  </thead>\n" +
            "  <tbody id=\"tbody\">\n" +
            "</table>\n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "<script>// Copyright (c) 2012 The Chromium Authors. All rights reserved.\n" +
            "// Use of this source code is governed by a BSD-style license that can be\n" +
            "// found in the LICENSE file.\n" +
            "\n" +
            "/**\n" +
            " * @fileoverview This file defines a singleton which provides access to all data\n" +
            " * that is available as soon as the page's resources are loaded (before DOM\n" +
            " * content has finished loading). This data includes both localized strings and\n" +
            " * any data that is important to have ready from a very early stage (e.g. things\n" +
            " * that must be displayed right away).\n" +
            " *\n" +
            " * Note that loadTimeData is not guaranteed to be consistent between page\n" +
            " * refreshes (https://crbug.com/740629) and should not contain values that might\n" +
            " * change if the page is re-opened later.\n" +
            " */\n" +
            "\n" +
            "/** @type {!LoadTimeData} */\n" +
            "// eslint-disable-next-line no-var\n" +
            "var loadTimeData;\n" +
            "\n" +
            "class LoadTimeData {\n" +
            "  constructor() {\n" +
            "    /** @type {?Object} */\n" +
            "    this.data_ = null;\n" +
            "  }\n" +
            "\n" +
            "  /**\n" +
            "   * Sets the backing object.\n" +
            "   *\n" +
            "   * Note that there is no getter for |data_| to discourage abuse of the form:\n" +
            "   *\n" +
            "   *     var value = loadTimeData.data()['key'];\n" +
            "   *\n" +
            "   * @param {Object} value The de-serialized page data.\n" +
            "   */\n" +
            "  set data(value) {\n" +
            "    expect(!this.data_, 'Re-setting data.');\n" +
            "    this.data_ = value;\n" +
            "  }\n" +
            "\n" +
            "  /**\n" +
            "   * @param {string} id An ID of a value that might exist.\n" +
            "   * @return {boolean} True if |id| is a key in the dictionary.\n" +
            "   */\n" +
            "  valueExists(id) {\n" +
            "    return id in this.data_;\n" +
            "  }\n" +
            "\n" +
            "  /**\n" +
            "   * Fetches a value, expecting that it exists.\n" +
            "   * @param {string} id The key that identifies the desired value.\n" +
            "   * @return {*} The corresponding value.\n" +
            "   */\n" +
            "  getValue(id) {\n" +
            "    expect(this.data_, 'No data. Did you remember to include strings.js?');\n" +
            "    const value = this.data_[id];\n" +
            "    expect(typeof value !== 'undefined', 'Could not find value for ' + id);\n" +
            "    return value;\n" +
            "  }\n" +
            "\n" +
            "  /**\n" +
            "   * As above, but also makes sure that the value is a string.\n" +
            "   * @param {string} id The key that identifies the desired string.\n" +
            "   * @return {string} The corresponding string value.\n" +
            "   */\n" +
            "  getString(id) {\n" +
            "    const value = this.getValue(id);\n" +
            "    expectIsType(id, value, 'string');\n" +
            "    return /** @type {string} */ (value);\n" +
            "  }\n" +
            "\n" +
            "  /**\n" +
            "   * Returns a formatted localized string where $1 to $9 are replaced by the\n" +
            "   * second to the tenth argument.\n" +
            "   * @param {string} id The ID of the string we want.\n" +
            "   * @param {...(string|number)} var_args The extra values to include in the\n" +
            "   *     formatted output.\n" +
            "   * @return {string} The formatted string.\n" +
            "   */\n" +
            "  getStringF(id, var_args) {\n" +
            "    const value = this.getString(id);\n" +
            "    if (!value) {\n" +
            "      return '';\n" +
            "    }\n" +
            "\n" +
            "    const args = Array.prototype.slice.call(arguments);\n" +
            "    args[0] = value;\n" +
            "    return this.substituteString.apply(this, args);\n" +
            "  }\n" +
            "\n" +
            "  /**\n" +
            "   * Returns a formatted localized string where $1 to $9 are replaced by the\n" +
            "   * second to the tenth argument. Any standalone $ signs must be escaped as\n" +
            "   * $$.\n" +
            "   * @param {string} label The label to substitute through.\n" +
            "   *     This is not an resource ID.\n" +
            "   * @param {...(string|number)} var_args The extra values to include in the\n" +
            "   *     formatted output.\n" +
            "   * @return {string} The formatted string.\n" +
            "   */\n" +
            "  substituteString(label, var_args) {\n" +
            "    const varArgs = arguments;\n" +
            "    return label.replace(/\\$(.|$|\\n)/g, function(m) {\n" +
            "      expect(m.match(/\\$[$1-9]/), 'Unescaped $ found in localized string.');\n" +
            "      return m === '$$' ? '$' : varArgs[m[1]];\n" +
            "    });\n" +
            "  }\n" +
            "\n" +
            "  /**\n" +
            "   * Returns a formatted string where $1 to $9 are replaced by the second to\n" +
            "   * tenth argument, split apart into a list of pieces describing how the\n" +
            "   * substitution was performed. Any standalone $ signs must be escaped as $$.\n" +
            "   * @param {string} label A localized string to substitute through.\n" +
            "   *     This is not an resource ID.\n" +
            "   * @param {...(string|number)} var_args The extra values to include in the\n" +
            "   *     formatted output.\n" +
            "   * @return {!Array<!{value: string, arg: (null|string)}>} The formatted\n" +
            "   *     string pieces.\n" +
            "   */\n" +
            "  getSubstitutedStringPieces(label, var_args) {\n" +
            "    const varArgs = arguments;\n" +
            "    // Split the string by separately matching all occurrences of $1-9 and of\n" +
            "    // non $1-9 pieces.\n" +
            "    const pieces = (label.match(/(\\$[1-9])|(([^$]|\\$([^1-9]|$))+)/g) ||\n" +
            "                    []).map(function(p) {\n" +
            "      // Pieces that are not $1-9 should be returned after replacing $$\n" +
            "      // with $.\n" +
            "      if (!p.match(/^\\$[1-9]$/)) {\n" +
            "        expect(\n" +
            "            (p.match(/\\$/g) || []).length % 2 === 0,\n" +
            "            'Unescaped $ found in localized string.');\n" +
            "        return {value: p.replace(/\\$\\$/g, '$'), arg: null};\n" +
            "      }\n" +
            "\n" +
            "      // Otherwise, return the substitution value.\n" +
            "      return {value: varArgs[p[1]], arg: p};\n" +
            "    });\n" +
            "\n" +
            "    return pieces;\n" +
            "  }\n" +
            "\n" +
            "  /**\n" +
            "   * As above, but also makes sure that the value is a boolean.\n" +
            "   * @param {string} id The key that identifies the desired boolean.\n" +
            "   * @return {boolean} The corresponding boolean value.\n" +
            "   */\n" +
            "  getBoolean(id) {\n" +
            "    const value = this.getValue(id);\n" +
            "    expectIsType(id, value, 'boolean');\n" +
            "    return /** @type {boolean} */ (value);\n" +
            "  }\n" +
            "\n" +
            "  /**\n" +
            "   * As above, but also makes sure that the value is an integer.\n" +
            "   * @param {string} id The key that identifies the desired number.\n" +
            "   * @return {number} The corresponding number value.\n" +
            "   */\n" +
            "  getInteger(id) {\n" +
            "    const value = this.getValue(id);\n" +
            "    expectIsType(id, value, 'number');\n" +
            "    expect(value === Math.floor(value), 'Number isn\\'t integer: ' + value);\n" +
            "    return /** @type {number} */ (value);\n" +
            "  }\n" +
            "\n" +
            "  /**\n" +
            "   * Override values in loadTimeData with the values found in |replacements|.\n" +
            "   * @param {Object} replacements The dictionary object of keys to replace.\n" +
            "   */\n" +
            "  overrideValues(replacements) {\n" +
            "    expect(\n" +
            "        typeof replacements === 'object',\n" +
            "        'Replacements must be a dictionary object.');\n" +
            "    for (const key in replacements) {\n" +
            "      this.data_[key] = replacements[key];\n" +
            "    }\n" +
            "  }\n" +
            "\n" +
            "  /**\n" +
            "   * Reset loadTimeData's data. Should only be used in tests.\n" +
            "   * @param {?Object} newData The data to restore to, when null restores to\n" +
            "   *    unset state.\n" +
            "   */\n" +
            "  resetForTesting(newData = null) {\n" +
            "    this.data_ = newData;\n" +
            "  }\n" +
            "\n" +
            "  /**\n" +
            "   * @return {boolean} Whether loadTimeData.data has been set.\n" +
            "   */\n" +
            "  isInitialized() {\n" +
            "    return this.data_ !== null;\n" +
            "  }\n" +
            "}\n" +
            "\n" +
            "  /**\n" +
            "   * Checks condition, throws error message if expectation fails.\n" +
            "   * @param {*} condition The condition to check for truthiness.\n" +
            "   * @param {string} message The message to display if the check fails.\n" +
            "   */\n" +
            "  function expect(condition, message) {\n" +
            "    if (!condition) {\n" +
            "      throw new Error(\n" +
            "          'Unexpected condition on ' + document.location.href + ': ' + message);\n" +
            "    }\n" +
            "  }\n" +
            "\n" +
            "  /**\n" +
            "   * Checks that the given value has the given type.\n" +
            "   * @param {string} id The id of the value (only used for error message).\n" +
            "   * @param {*} value The value to check the type on.\n" +
            "   * @param {string} type The type we expect |value| to be.\n" +
            "   */\n" +
            "  function expectIsType(id, value, type) {\n" +
            "    expect(\n" +
            "        typeof value === type, '[' + value + '] (' + id + ') is not a ' + type);\n" +
            "  }\n" +
            "\n" +
            "  expect(!loadTimeData, 'should only include this file once');\n" +
            "  loadTimeData = new LoadTimeData;\n" +
            "\n" +
            "  // Expose |loadTimeData| directly on |window|, since within a JS module the\n" +
            "  // scope is local and not all files have been updated to import the exported\n" +
            "  // |loadTimeData| explicitly.\n" +
            "  window.loadTimeData = loadTimeData;\n" +
            "\n" +
            "  console.warn('crbug/1173575, non-JS module files deprecated.');</script><script>loadTimeData.data = {\"header\":\"LOCATION의 색인\",\"headerDateModified\":\"수정된 날짜\",\"headerName\":\"이름\",\"headerSize\":\"크기\",\"language\":\"ko\",\"listingParsingErrorBoxText\":\"이 서버가 Chrome에서 이해할 수 없는 데이터를 전송합니다. \\u003Ca href=\\\"http://code.google.com/p/chromium/issues/entry\\\">버그를 신고\\u003C/a>하고 \\u003Ca href=\\\"LOCATION\\\">원본 목록\\u003C/a>을 포함하세요.\",\"parentDirText\":\"[상위 디렉터리]\",\"textdirection\":\"ltr\"};</script><script>start(\"$FILE/\");</script>\n" +
            "<script>onHasParentDirectory();</script>";
}
