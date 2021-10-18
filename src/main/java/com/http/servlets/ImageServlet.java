/*
package com.http.servlets;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Collection;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Size; 
import org.opencv.core.Point; 


import com.utils.Utils;

import nu.pattern.OpenCV;

import com.utils.MimeTypes;

@MultipartConfig
@WebServlet(urlPatterns = { "/image/*" }, asyncSupported = true, loadOnStartup = 0)
public class ImageServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private static final String folderToSave = "/images";
    private static File directory;

    private static final int BUFFER_SIZE = 4096;

    public ImageServlet() {
        super();
        
        File dir = new File(folderToSave);
        
        if (!dir.exists() || !dir.isDirectory()) {
        	if (dir.mkdir()) {
        		System.out.println("Images save folder created at [" + dir.getAbsolutePath() +  "]");
        	} else {
        		System.err.println("Images save folder not created!");
        	}
        } else {
        	System.out.println("Images save folder exists at [" + dir.getAbsolutePath() +  "]");
        }
        
        directory = dir;
        OpenCV.loadLocally();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String[] path = request.getPathInfo().split("/");
		
		if (path.length < 2) {
			response.getWriter().write("{ error: true, message: \"invalid request\" }");
			return;
		}
		
		String action = path[1];
		System.out.println("Processing action [" + action + "]");
		
		if (action.equals("upload")) {
			for (File f : directory.listFiles()) f.delete();
			
			String images = request.getParameter("images");
			System.out.println(images);
			
			if (images == null) {
				response.sendError(400);
				return;
			}
			
			String[] urls = images.trim().split(",");
			int l = urls.length;
			
			for (int i = 0; i < l; i++) {
				String fileURL = urls[i].trim();
				if (fileURL.isEmpty()) continue;
				
				System.out.println(fileURL);
				URL url = new URL(fileURL);
				HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
				int responseCode = httpConn.getResponseCode();
				
				if (responseCode == HttpURLConnection.HTTP_OK) {
		            String fileName = "";
		            String disposition = httpConn.getHeaderField("Content-Disposition");
		            String contentType = httpConn.getContentType();
		            int contentLength = httpConn.getContentLength();
		 
		            if (disposition != null) {
		                int index = disposition.indexOf("filename=");
		                
		                if (index > 0) {
		                    fileName = disposition.substring(index + 10, disposition.length() - 1);
		                }
		            } else {
		                // extracts file name from URL
		                fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1, fileURL.length());
		            }
		 
		            System.out.println("Content-Type = " + contentType);
		            System.out.println("Content-Disposition = " + disposition);
		            System.out.println("Content-Length = " + contentLength);
		            System.out.println("fileName = " + fileName);
		 
		            InputStream inputStream = httpConn.getInputStream();
		            String saveFilePath = directory.getAbsolutePath() + File.separator + fileName;
		             
		            FileOutputStream outputStream = new FileOutputStream(saveFilePath);
		 
		            int bytesRead = -1;
		            byte[] buffer = new byte[BUFFER_SIZE];
		            
		            while ((bytesRead = inputStream.read(buffer)) != -1) {
		                outputStream.write(buffer, 0, bytesRead);
		            }
		 
		            outputStream.close();
		            inputStream.close();
		        }
			}
			
			System.out.println("Copied " + directory.listFiles().length);
			for (File f : directory.listFiles()) removeWatermark(f);
		} else if (action.contentEquals("process")) {
			if (directory.listFiles().length > 0) {
				for (File f : directory.listFiles()) {
					removeWatermark(f);
					break;
				}
				
				response.getWriter().write("{ success: true, message: \"watermark remove initiated\" }");
				return;
			}
			
			response.getWriter().write("{ error: true, message: \"no files in the upload directory\" }");
			return;
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	private Mat getCropped(Mat source) {
		Mat grayscale = new Mat();
		Mat thresh = new Mat();
		Mat points = new Mat();
		
		Imgproc.cvtColor(source, grayscale, Imgproc.COLOR_BGR2GRAY);
		Imgproc.threshold(grayscale, thresh, 236, 255, Imgproc.THRESH_BINARY_INV);
		Core.findNonZero(thresh, points);
		//System.out.println(points.dump());
		Rect rect = Imgproc.boundingRect(points);
		System.out.println(rect.toString());
		return new Mat(source, rect);
	}
	
	private Mat getCropped(Mat source, int x, int y) {
		Mat grayscale = new Mat();
		Mat thresh = new Mat();
		Mat points = new Mat();
		
		Imgproc.cvtColor(source, grayscale, Imgproc.COLOR_BGR2GRAY);
		Imgproc.threshold(grayscale, thresh, 236, 255, Imgproc.THRESH_BINARY_INV);
		Core.findNonZero(thresh, points);
		//System.out.println(points.dump());
		Rect rect = new Rect(0, 0, x, y);
		System.out.println(rect.toString());
		return new Mat(source, rect);
	}
	
	private void removeWatermark(File input) {
		String inputname = input.getAbsolutePath().split("\\.(?=[^\\.]+$)")[0];
		String inputext = input.getAbsolutePath().split("\\.(?=[^\\.]+$)")[1];
		
		Mat file = Imgcodecs.imread(input.getAbsolutePath());
		Mat cropped = getCropped(file);
		
		final int width = cropped.width();
		final int height = cropped.height();
		final int offsetX = 640 - width;
		final int offsetY = 480 - height;
		
		Mat img = new Mat();
		Core.copyMakeBorder(cropped, img, 0, offsetY, 0, offsetX, Core.BORDER_DEFAULT, new Scalar(0, 255, 0));
		//Imgcodecs.imwrite(inputname + "_3." + inputext, img);
		
		Size size = img.size();
		System.out.println("Got image of size " + size.width + "x" + size.height);
		
		Mat copy = new Mat();
		img.copyTo(copy);
		
		double tan = 0.7;
		double x1 = 187;
		double a1 = size.width - x1;
		double y1 = size.height - a1 * tan;
		double offset = 38;
		double x2 = x1 + offset;
		double a2 = size.width - x2;
		double y2 = size.height - a2 * tan;
		
		MatOfPoint pointsTop = new MatOfPoint(
			new Point(0, 0), 
			new Point(size.width, 0),
			new Point(size.width, y1),
			new Point(x1, size.height),
			new Point(0, size.height)); 
		
		Imgproc.fillConvexPoly(copy, pointsTop, new Scalar(255, 255, 255));
		
		MatOfPoint pointsBottom = new MatOfPoint(
			new Point(x2, size.height),
			new Point(size.width, y2), 
			new Point(size.width, size.height)); 

		Imgproc.fillConvexPoly(copy, pointsBottom, new Scalar(255, 255, 255));
		//Imgcodecs.imwrite(inputname + "_1." + inputext, copy);
		
		Mat grayscale = new Mat();
		Imgproc.cvtColor(copy, grayscale, Imgproc.COLOR_BGR2GRAY);
		//Imgcodecs.imwrite(inputname + "_2." + inputext, grayscale);

		Scalar mean = Core.mean(grayscale);
		double m = mean.val[0];
		System.out.println("mean is " + m);
		
		Mat thresh = new Mat();
		Imgproc.threshold(grayscale, thresh, m, 255, Imgproc.THRESH_TRUNC);
		//Imgcodecs.imwrite(inputname + "_3." + inputext, thresh);
		Mat kernel = Mat.ones(5, 5, CvType.CV_8UC1);
		
		Mat morph = new Mat();
		Mat morph1 = new Mat();
		Imgproc.morphologyEx(thresh, morph, Imgproc.MORPH_CLOSE, kernel);
		//Imgcodecs.imwrite(inputname + "_morph1." + inputext, morph);
		Imgproc.morphologyEx(morph, morph1, Imgproc.MORPH_OPEN, kernel);
		//Imgcodecs.imwrite(inputname + "_morph2." + inputext, morph);
		
		kernel = Mat.ones(3, 3, CvType.CV_8UC1);
		Imgproc.morphologyEx(morph1, morph, Imgproc.MORPH_OPEN, kernel);
		//Imgcodecs.imwrite(inputname + "_morp3." + inputext, morph);
		//Mat thresh = new Mat();
		//Imgproc.threshold(grayscale, thresh, 130, 255, Imgproc.THRESH_BINARY);
		
		Mat subtract = new Mat();
		Core.subtract(morph, thresh, subtract);
		//Imgcodecs.imwrite(inputname + "_5." + inputext, subtract);
		
		Mat thresh_pre = new Mat();
		Imgproc.threshold(subtract, thresh_pre, 10, 255, Imgproc.THRESH_BINARY);
		//Imgcodecs.imwrite(inputname + "_6." + inputext, thresh_pre);
		
		Imgproc.fillConvexPoly(thresh_pre, pointsTop, new Scalar(255, 255, 255));
		Imgproc.fillConvexPoly(thresh_pre, pointsBottom, new Scalar(255, 255, 255));
		//Imgcodecs.imwrite(inputname + "_7." + inputext, thresh_pre);
		
		Mat invert = new Mat();
		Core.bitwise_not(thresh_pre, invert);
		//Imgcodecs.imwrite(inputname + "_8." + inputext, invert);
		
		kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
		Mat dilate = new Mat();
		Point kp = new Point(-1.0, -1.0);
		Imgproc.dilate(invert, dilate, kernel, kp, 2);
		//Imgcodecs.imwrite(inputname + "_9." + inputext, dilate);
		
		Mat result = new Mat();
		Photo.inpaint(img, dilate, result, 32, Photo.INPAINT_TELEA); 
		Imgcodecs.imwrite(inputname + "_result." + inputext, getCropped(result, width, height));
		System.out.println("finished removal");
		
		Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2BGRA);
		
		Mat text = new Mat(result.height() * 2, result.width() * 2, CvType.CV_8UC3, colorRGB(0, 0, 0));
		Imgproc.cvtColor(text, text, Imgproc.COLOR_BGR2BGRA);
		
		Imgproc.putText(
			text, 
			"Autorazborka.by*Autorazborka.by*Autorazborka.by*Autorazborka.by", 
			new Point(0, size.height * 1.29), 
			Imgproc.FONT_HERSHEY_COMPLEX, 
			1, 
			colorRGB(255, 255, 255), 
			3, 
			Imgproc.LINE_AA, 
			false);
		
		//Imgcodecs.imwrite(inputname + "_textblock." + inputext, text);
		Mat rotatedText = new Mat();
		Mat matrix = Imgproc.getRotationMatrix2D(new Point(result.width(), result.height()), 35, 1);
		
		Imgproc.warpAffine(text, 
			rotatedText, 
			matrix, 
			new Size(text.width(), text.height()), 
			Imgproc.INTER_LINEAR, 
			Core.BORDER_TRANSPARENT, 
			new Scalar(0, 0, 0, 255));
		
		Rect rectCrop = new Rect((rotatedText.width() - result.width()) / 2, (rotatedText.height() - result.height()) / 2, result.width(), result.height());
		Mat roi = new Mat(rotatedText, rectCrop);
		
		Core.addWeighted(result, 1, roi, 0.25, 0, result); 
		Imgcodecs.imwrite(inputname + "_text." + inputext, getCropped(result, width, height));
		System.out.println("finished text add");
	}
	
	private void writeFiles(HttpServletRequest request) {
		System.out.println("Request content type is " + request.getContentType());
		
		if (request.getContentType() == null || !request.getContentType().contains("multipart/form-data")) {	
			System.err.println("Request content type invalid");
			return;
		}
		
		try {
			int n = 0;
			Collection<Part> parts = request.getParts();
			if (parts == null) return;
			
			for (Part part : parts) {
				if (part.getName().equalsIgnoreCase("uploaded_image") && part.getSize() > 0L) {
					String e = MimeTypes.getExtension(part.getContentType(), true);
					String name = Utils.genRandomString(16);
					
					File f = new File(directory.getAbsolutePath(), name + "."  + e);
					if (!f.exists() && !f.createNewFile()) continue;
					part.write(f.getPath());
					n++;
				} else {
					System.out.println("Ignoring part " + part.getName() + " of size " + part.getSize());
				}
			}
			
			System.out.print(n + " files saved");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Scalar colorRGB(double red, double green, double blue) {
		return new Scalar(blue, green, red);
	} 
}
*/
