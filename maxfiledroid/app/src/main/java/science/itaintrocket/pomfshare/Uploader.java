package science.itaintrocket.pomfshare;

import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.webkit.MimeTypeMap;


public class Uploader extends AsyncTask<String, Integer, String>{
	private final String pomfurl = "https://maxfile.ro/upload.php?output=gyazo";
	private final String uguuurl = "http://uguu.se/api.php?d=upload";
	private final String boundary = "*****";
	private final int maxBufferSize = 1024*1024;
	private final String tag = "ayy lmao";
	
	private ParcelFileDescriptor file;
	private MainActivity source;
	private Service service;

	public enum Service { POMF, UGUU }

	public Uploader(MainActivity sender, ParcelFileDescriptor pfd, Service service) {
		this.source = sender;
		this.file = pfd;
		this.service = service;
	}

	@Override
	protected String doInBackground(String... params) {
		String filename = params[0];
		String contentType = params[1];
		String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType);
		String uploadurl = (service == Service.POMF) ? pomfurl : uguuurl;
		String fieldName = (service == Service.POMF) ? "files[]" : "file";

		String result = null;
		try {

		    HttpURLConnection conn = (HttpURLConnection) new URL(uploadurl).openConnection();

		    conn.setDoInput(true);
		    conn.setDoOutput(true);
		    conn.setUseCaches(false);

		    conn.setRequestMethod("POST");

		    conn.setRequestProperty("Connection", "Keep-Alive");
		    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);

		    DataOutputStream out = new DataOutputStream(conn.getOutputStream());
		    out.writeBytes("--" + boundary + "\r\n");
		    out.writeBytes(String.format("Content-Disposition: form-data; name=\"%s\";filename=\"%s.%s\"\r\nContent-type: %s\r\n",
		    		fieldName, filename, extension, contentType));
		    out.writeBytes("\r\n");
		    
		    Log.d(tag, filename + "." + extension);
		    
			FileInputStream fileInputStream = new FileInputStream(file.getFileDescriptor());
		 
		    int bytesAvailable = fileInputStream.available();
		    int bufferSize = Math.min(bytesAvailable, maxBufferSize);
		    byte[] buffer = new byte[bufferSize];
		 
		    Log.d(tag, "Pre-read file " + fileInputStream);
		    // Read file
		    int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
		 
		    while (bytesRead > 0)
		    {
		        out.write(buffer, 0, bytesRead);
		        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
		    }

		    Log.d(tag, "Post-read file");
		    out.writeBytes("\r\n");
		    out.writeBytes("--" + boundary + "--\r\n");
		 
		    // Responses from the server (code and message)
		    int responseCode = conn.getResponseCode();
		    String responseMessage = conn.getResponseMessage();
		    
		    Scanner reader = new Scanner(conn.getInputStream());
		    result = reader.nextLine();

		    Log.d(tag, String.format("%d: %s", responseCode, responseMessage));
		    Log.d(tag, result);		    
		 
		    fileInputStream.close();
		    reader.close();
		    out.flush();
		    out.close();
		    
		} catch(IOException e) {
			Log.e(tag, e.getMessage());
			return String.format("Upload failed, check your internet connection (%s)", e.getMessage());
		}

	    return result;
	}
	
	@Override
	protected void onPostExecute(String result) {
		source.finishUpload(result);
	}
	

}