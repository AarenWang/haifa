package org.wrj.haifa.nio.httpserver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import com.samskivert.mustache.Mustache;

class FileItem implements Comparable<FileItem>{
    public final String href;
    public final String name;
    public final String size;
    public final String mtime;

    public FileItem(String href, String name, String size, String mtime) {
        this.href = href;
        this.name = name;
        this.size = size;
        this.mtime = mtime;
    }
    @Override
    public int compareTo(FileItem o) {
        return name.compareTo(o.name);
    }
}

public class Util {

    private static String defaultType = "application/octet-stream";
    private static String mapFile = "mime.types";
    private static String indexTmpl = "index.tpl";
    private static DateFormat df = new SimpleDateFormat("yyyy-HH-dd HH:mm:ss");

    public static String getExtension(File file) {
        String name = file.getName();
        int index = name.lastIndexOf('.');
        if (index != -1)
            return name.substring(index + 1).toLowerCase();
        else
            return "";

    }

    public static Object listDir(final File folder) {
        File[] files = folder.listFiles();
        final List<FileItem> fileItems = new ArrayList<FileItem>();
        for (File file : files) {
            String href = file.isDirectory() ? file.getName() + "/" : file
                    .getName();
            String mtime = df.format(new Date(file.lastModified()));
            fileItems.add(new FileItem(href, file.getName(),
                    file.length() + "", mtime));
        }
        Collections.sort(fileItems);
        return new Object() {
            Object files = fileItems;
            Object dir = folder.getName();
        };
    }

    public static byte[] directoryList(File dir, boolean zip) {
        StringBuilder sb = new StringBuilder(300);

        InputStream ins = Util.class.getClassLoader().getResourceAsStream(
                indexTmpl);
        BufferedReader br = new BufferedReader(new InputStreamReader(ins));
        String line = null;
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (IOException e) {
        }

        String html = Mustache.compiler().compile(sb.toString())
                .execute(listDir(dir));

        if (zip) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(8912);
                GZIPOutputStream gzip = new GZIPOutputStream(baos);
                gzip.write(html.getBytes());
                closeQuietly(gzip);
                return baos.toByteArray();
            } catch (IOException e) {
            }
        } else {
            return html.getBytes();
        }
        return new byte[] {};
    }

    public static String getContentType(File file) {

        if (file.isDirectory())
            return "text/html";

        InputStream ins = Util.class.getClassLoader().getResourceAsStream(
                mapFile);

        String exten = getExtension(file);
        Map<String, String> map = new HashMap<String, String>();

        try {
            BufferedReader bis = new BufferedReader(new InputStreamReader(ins));
            String line = null;
            while ((line = bis.readLine()) != null) {
                String[] tmp = line.split("\\s+");
                map.put(tmp[0], tmp[1]);
            }
        } catch (IOException e) {
        }

        if (map.get(exten) == null)
            return defaultType;
        else
            return map.get(exten);

    }

    public static void closeQuietly(Closeable is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     *
     * @param file
     *            the absolute file path
     * @param zip
     *            gzip or not
     * @return byte array of the file
     *
     * @throws IOException
     */
    public static byte[] file2ByteArray(File file, boolean zip)
            throws IOException {
        if (file.isFile()) {
            InputStream is = null;
            GZIPOutputStream gzip = null;
            byte[] buffer = new byte[8912];
            ByteArrayOutputStream baos = new ByteArrayOutputStream(8912);
            try {
                if (zip) {
                    gzip = new GZIPOutputStream(baos);
                }
                is = new BufferedInputStream(new FileInputStream(file));
                int read = 0;
                while ((read = is.read(buffer)) != -1) {
                    if (zip) {
                        gzip.write(buffer, 0, read);
                    } else {
                        baos.write(buffer, 0, read);
                    }
                }
            } catch (IOException e) {
                throw e;
            } finally {
                closeQuietly(is);
                closeQuietly(gzip);
            }
            return baos.toByteArray();
        } else if (file.isDirectory()) {
            return directoryList(file, zip);
        } else {
            return new byte[] {};
        }
    }

    /**
     * same as {@link Util#subArray(byte[], byte[], int)},except find from end
     * to start;
     *
     * @param data
     *            to search from
     * @param tofind
     *            target
     * @param start
     *            start index
     * @return index of the first find if find, data.length if not find
     */
    public static int subArrayFromEnd(byte[] data, byte[] tofind, int start) {
        int index = data.length;
        outer: for (int i = data.length - tofind.length; i > 0; --i) {

            for (int j = 0; j < tofind.length;) {
                if (data[i] == tofind[j]) {
                    ++i;
                    ++j;
                    if (j == tofind.length) {
                        index = i - tofind.length;
                        break outer;
                    }
                } else {
                    i = i - j; // step back
                    break;
                }
            }
        }
        return index;
    }

    /**
     *
     * @param data
     *            to search from
     * @param tofind
     *            target
     * @param start
     *            start index
     * @return index of the first find if find, data.length if not find
     */
    public static int subArray(byte[] data, byte[] tofind, int start) {
        int index = data.length;
        outer: for (int i = start; i < data.length; ++i) {

            for (int j = 0; j < tofind.length;) {
                if (data[i] == tofind[j]) {
                    ++i;
                    ++j;
                    if (j == tofind.length) {
                        index = i - tofind.length;
                        break outer;
                    }
                } else {
                    i = i - j; // step back
                    break;
                }
            }
        }
        return index;
    }
}