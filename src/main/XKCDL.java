package main;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import visionCore.io.MultiPrintStream;
import visionCore.util.Files;
import visionCore.util.Web;

public class XKCDL {
	
	public static String abspath;

	public static void main(String[] args) {
		
		try {
			
			abspath = new File(XKCDL.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath().replace("\\", "/");
			
			if (abspath.endsWith("/bin")) {
				
				abspath = abspath.substring(0, abspath.indexOf("/bin"));
			}
			
			if (abspath.endsWith(".jar")) {
				
				abspath = new File(abspath).getParentFile().getAbsolutePath();
			}
			
		} catch (Exception e) { e.printStackTrace(); }
		
		setOutStream();
		
		String path = getPath(args);
		if (path == null || path.length() < 1) { return; }
		
		File dir = new File(path);
		if (!dir.exists()) { dir.mkdirs(); }
		
		int comicsCount = getComicsCount();
		
		List<File> imgs = Files.getFilesRecursive(dir.getAbsolutePath(), f -> (f.getName().toLowerCase().endsWith(".png") || 
																f.getName().toLowerCase().endsWith(".png")) && !f.isDirectory());
		
		Set<Integer> saved = new HashSet<Integer>();
		
		for (File f : imgs) {
			
			String n = f.getName().toLowerCase().trim();
			n = n.substring(0, n.lastIndexOf("."));
			
			if (n.contains(" -")) {
				
				n = n.substring(0, n.indexOf(" -"));
			}
			
			int i = Integer.parseInt(n);
			saved.add(i);
		}
		
		System.out.println("\nDownloading "+(comicsCount-saved.size() - 7)+" comics:\n");
		
		ExecutorService exec = Executors.newFixedThreadPool(16);
		
		try {
			
			for (int i = 1; i <= comicsCount; i++) {
				if (saved.contains(i)) { continue; }
				
				final int ind = i;
				
				exec.submit(new Runnable(){
					
					@Override
					public void run() {
						
						String html = Web.getHTML("http://xkcd.com/"+ind, false);
						
						String f = "<div id=\"ctitle\">";
						html = html.substring(html.toLowerCase().indexOf(f)+f.length());
						
						String title = html.substring(0, html.toLowerCase().indexOf("</div>"));
						title = title.trim();
						
						f = "<div id=\"comic\">";
						html = html.substring(html.toLowerCase().indexOf(f)+f.length());
						
						f = "<img src=\"//";
						html = html.substring(html.toLowerCase().indexOf(f)+f.length());
						
						String imgurl = html.substring(0, html.toLowerCase().indexOf("\" title"));
						imgurl = "http://"+imgurl;
						
						BufferedImage img = Web.getImage(imgurl);
						
						
						int lb = (ind / 200) * 200;
						String subdir = lb+" - "+(lb+199);
						
						
						String indStr = ind+"";
						for (int j = 0, l = indStr.length(); j < 4 - l; l++) { indStr = "0"+indStr; }
						
						String fileName = Files.cleanName(indStr+" - "+title+".png");
						
						File out = new File(dir.getAbsolutePath()+"/"+subdir+"/"+fileName);
						if (out.exists()) { return; }
						if (!out.getParentFile().exists()) { out.getParentFile().mkdirs(); }
						
						try { ImageIO.write(img, "png", out); } catch (IOException e) { e.printStackTrace(); }
						
						System.out.println("Saved "+indStr+" - "+title);
						
					}
					
				});
				
			}
			
		} finally { exec.shutdown(); }
		
		try {
			
			exec.awaitTermination(4, TimeUnit.HOURS);
		} catch (Exception | Error e) { e.printStackTrace(); }
		
		System.out.println("\nAll done!");
		
	}
	
	private static String getPath(String[] args) {
		
		String path = "";
		
		File cfg = new File(abspath+"/cfg");
		
		try {
			
			if (cfg.exists()) { 
				
				BufferedReader br = new BufferedReader(new FileReader(cfg));
				
				for (String line = ""; (line = br.readLine()) != null;) {
					line = line.trim();
					
					path = line;
				}
				
				br.close();
			}
			
		} catch (Exception e) { e.printStackTrace(); }
		
		if (args.length > 0 && args[0] != null) {
			
			path = args[0];
		}
		
		path = path.replace("\\", "");
		path = path.replace("\"", "");
		
		if (cfg.exists()) { cfg.delete(); }
		Files.writeText(new File(abspath+"/cfg"), path);
		
		return path;
	}
	
	private static int getComicsCount() {
		
		String html = Web.getHTML("http://xkcd.com/", false);
		
		String f = "<ul class=\"comicnav\">";
		html = html.substring(html.toLowerCase().indexOf(f)+f.length());
		
		f = "<a rel=\"prev\" href=\"/";
		html = html.substring(html.toLowerCase().indexOf(f)+f.length());
		
		String n = html.substring(0, html.toLowerCase().indexOf("/"));
		
		return Integer.parseInt(n)+1;
	}
	
	public static void setOutStream() {
		
		try {
			
			File logfile = new File(abspath+"/log.txt");
			
			PrintStream logOut = new PrintStream(new FileOutputStream(logfile));
			
			PrintStream multiOut = new MultiPrintStream(logOut, System.out);
			PrintStream multiErr = new MultiPrintStream(logOut, System.err);
			
			System.setOut(multiOut);
			System.setErr(multiErr);
			
		} catch (Exception | Error e) { e.printStackTrace(); }
		
	}
	
}
