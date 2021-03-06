package github.aq.audiofiledownloader.download;

import github.aq.audiofiledownloader.model.AudioURL;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileDownloader {
    
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36";
    private static final int BUFFER_SIZE = 4096;
    private static final String HTML_INDEX = "index.html";
    
    private static Path fileDownloadPath;
    public static int requestTimeWait = 2;
    
    /**
     * Simple class for collecting the failures
     */
    public static class DownloadExecutionSummary {
            
        public static int totalDownloads = 0;
        public static int totalFailedDownloads = 0;
        public static int totalDownloadsWithZeroContentSize = 0;

        public static String report() {
            return "Downloads number: " + totalDownloads + ", downloads with empty content: "+ totalDownloadsWithZeroContentSize + ", downloads failed: " + totalFailedDownloads;
        }
    }
    
    public static class Download {
        public static String currentFilename = "";
        public static int contentLength = 0;
        public static int responseCode = 0;
        public static String contentType; 
        
        public static String report() {
            return "Current filename: " + currentFilename + ", response code: " + responseCode + ", contentLength: " + contentLength + ", contentType: " + contentType;
        }
    }

    /**
     * Was the target save file path provided ?
     * @param fileURL       String contains the file url
     * @param saveFilePathTarget    download location
     * @return  The relative host path to create locally on disk
     * @throws MalformedURLException    in case the fileURL is malformed
     */
    public static Path getStoragePath(String fileURL, String saveFilePathTarget) throws MalformedURLException {
        if ("".equals(saveFilePathTarget)) {
            return Paths.get(new URL(fileURL).getHost() + new AudioURL(fileURL).getPath());
        } else {
            return Paths.get(saveFilePathTarget, new URL(fileURL).getHost() + new AudioURL(fileURL).getPath());
        }
    }
    
    /**
     * Does the download path exist on the disk ?
     * @param downloadPath  The download location 
     * @throws IOException  in case of an directory creation issue 
     */
    public static void createDirectoryIfNotExists(Path downloadPath) throws IOException {
        if (Files.notExists(downloadPath)) {
            Files.createDirectories(downloadPath);
        }
    }
    
    /**
     * parse the name of the file if the url ends with / filename is index.html
     * @param fileURL String contains the file url
     * @return  The name of the filename
     * @throws MalformedURLException    in case of an exception
     * @throws UnsupportedEncodingException in a case of an exception
     */
    public static String parseFilename(String fileURL) throws MalformedURLException, UnsupportedEncodingException {
        String fileName = Paths.get(new URL(fileURL).getFile()).getFileName().toString();
        fileName = java.net.URLDecoder.decode(fileName, StandardCharsets.UTF_8.displayName());
        if (fileURL.endsWith("/")) {
            fileName = HTML_INDEX;
        }
        return fileName;
    }
    
    /**
     * construct the absolute file download path
     * @param localDownloadPath   the local download path
     * @param fileName   String contains the filename 
     * @return  the absolute file download path and filename
     */
    public static String getAbsoluteFileDownloadPath(Path localDownloadPath, String fileName) {
        return localDownloadPath.toString() + File.separator + fileName;
    }
    
    /**
     * download the file to disk in target save file path
     * @param fileURL   String contains the file url
     * @param targetSaveFilePath the location to download the file
     * @return true if file was downloaded with content length > 0
     */
    public static boolean downloadFile(String fileURL, String targetSaveFilePath) {
        boolean isFileExist = false; 
        try {
            Path localDownloadPath = null;  
            DownloadExecutionSummary.totalDownloads ++;
            
            // Parse the filename of the URL
            String fileName = parseFilename(fileURL);
            
            // Was the target save file path provided ?
            localDownloadPath = getStoragePath(fileURL, targetSaveFilePath);

            // Does the download path exist on the disk ?
            createDirectoryIfNotExists(localDownloadPath);
        
            fileDownloadPath = Paths.get(getAbsoluteFileDownloadPath(localDownloadPath, fileName));
            
            // Does file already exist ?
            if (Files.notExists(fileDownloadPath)) {

                // start download
                URL url = new URL(fileURL);	        
                HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                httpConn.setRequestMethod("GET"); 
                httpConn.setRequestProperty("User-Agent", USER_AGENT);
                Download.responseCode = httpConn.getResponseCode();
                Download.currentFilename = getAbsoluteFileDownloadPath(localDownloadPath, fileName);
                // always check HTTP response code first
                if (Download.responseCode == HttpURLConnection.HTTP_OK) {
                    
                    Download.contentType = httpConn.getContentType();
                    Download.contentLength = httpConn.getContentLength();

                    // is file URL a folder ?
                    if (fileURL.endsWith("/") && Download.contentType.contains("text/html;")) {
                        fileName = HTML_INDEX;
                    }

                    // opens input stream from the HTTP connection
                    InputStream inputStream = httpConn.getInputStream();

                    // opens an output stream to save into file
                    FileOutputStream outputStream = new FileOutputStream(getAbsoluteFileDownloadPath(localDownloadPath, fileName));//.replaceAll("[\\s]", "")));

                    int bytesRead = -1;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    outputStream.close();
                    inputStream.close();

                    System.out.println(Download.report());

                    if (Download.contentLength == 0) {
                        DownloadExecutionSummary.totalDownloadsWithZeroContentSize ++;
                    }

                    try {
                        Thread.sleep(requestTimeWait * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                } else {
                    System.out.println("Failure - " + Download.report());
                    DownloadExecutionSummary.totalFailedDownloads ++;
                    return false;
                }
                httpConn.disconnect();
                
            } else {
                System.out.println("File found on disk - " + getAbsoluteFileDownloadPath(localDownloadPath, fileName));
                isFileExist = true;
            }
            
        } catch (IOException exc) {
            exc.printStackTrace();
        }
        
        return Download.contentLength > 0 || isFileExist;
    }

    public static Path getDownloadPath() {
        return fileDownloadPath;
    }

}
