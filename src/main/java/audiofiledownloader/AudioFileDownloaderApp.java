package audiofiledownloader;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import audiofiledownloader.download.FileDownloader;
import audiofiledownloader.parse.AudioFileParser;

public class AudioFileDownloaderApp {

	public static void main(String[] args) {
		
		String url = "";
		String timeWait = "";
        Options options = new Options();
        
        options.addOption(Option.builder("url")
				.required(true)
				.hasArg(true).argName("url")
				.desc("the url to get the files").build());
        
        options.addOption(Option.builder("time_wait")
				.required(false)
				.hasArg(true).argName("time_wait")
				.desc("the time to wait between requests in seconds").build());
        
		try {
			CommandLine commandLine = new DefaultParser().parse(options, args);
			url = commandLine.getOptionValue("url");
			timeWait = commandLine.getOptionValue("time_wait");
			
			try {				
				if (timeWait != null) {
					FileDownloader.REQ_TIME_WAIT = Integer.parseInt(timeWait);
				} 
			} catch (NumberFormatException exc) {
				exc.printStackTrace();
			}
				
			processDownload(url, "");
			System.out.println("Downloads number: " + FileDownloader.getTotalDownloads() + " - failed: " + FileDownloader.getTotalFailedDownloads());
			
		} catch (ParseException e) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.setOptionComparator(null); // Keep insertion order of options
			formatter.printHelp(AudioFileDownloaderApp.class.getName(), "download audio files from url.", options, null);
			System.exit(1);
			return;
		}
	}
	
	public static void processDownload(String url, String parentPath) {
		if (FileDownloader.downloadFile(url, parentPath)) {
			if (!FileDownloader.getDownloadPath().toString().matches(".*mp3|.*mp4")) {
				List<String> listResources = AudioFileParser.parse(FileDownloader.getDownloadPath());
				if (listResources.size() > 0) {
					for(String resource: listResources) {
						if (resource.matches(".*mp3|.*mp4")) {
							if (FileDownloader.downloadFile(url + resource, FileDownloader.getDownloadPath().toString())) {
								continue;
							} else {
								processDownload(resource, FileDownloader.getDownloadPath().toString());
							}
						}
					}
				}
			}
		}
	}

}
