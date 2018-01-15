package hl.crawler.social.facebook;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import hl.common.ImgUtil;

public class FBUser {

	/**
	profile.id=677806677
	profile.name=ONG Hui Lam
	profile.url=https://www.facebook.com/onghuilam
	photo.profile.url=http://aaaa/1932380_profile.jpg
	photo.profile.cache=1932380_profile.jpg
	photo.cover=
	 */
	private String profile_id 	= null;
	private String profile_name = null;
	private String profile_url 	= null;
	private Date profile_crawled_time 		= null;
	private Date profile_last_updated_time 	= null;
	
	private String userdata_root_path 		= null;
	
	private String photo_profile_url 		= null;
	private String photo_cover_url 			= null;
	
	private String photo_profile_cache 		= null;
	private String photo_cover_cache 		= null;
	
	private BufferedImage photo_profile_image 	= null;
	private BufferedImage photo_cover_image 	= null;

	public String getProfile_id() {
		return profile_id;
	}
	public void setProfile_id(String profile_id) {
		this.profile_id = profile_id;
	}
	public String getProfile_name() {
		return profile_name;
	}
	public void setProfile_name(String profile_name) {
		this.profile_name = profile_name;
	}
	public String getProfile_url() {
		return profile_url;
	}
	public void setProfile_url(String profile_url) {
		this.profile_url = profile_url;
	}
	
	public Date getProfile_last_updated_time() {
		return profile_last_updated_time;
	}
	public void setProfile_last_updated_time(Date profile_last_updated_time) {
		this.profile_last_updated_time = profile_last_updated_time;
	}
	
	public String getProfile_last_updated_time(String aDateFormat) {
		if(getProfile_last_updated_time()==null)
			return null;
		SimpleDateFormat df = new SimpleDateFormat(aDateFormat);
		return df.format(getProfile_last_updated_time());
	}
	
	public void setProfile_last_updated_time(String aProfile_last_updated_time, String aDateFormat) throws ParseException {
		SimpleDateFormat df = new SimpleDateFormat(aDateFormat);
		setProfile_last_updated_time(df.parse(aProfile_last_updated_time));
	}
	
	public Date getProfile_crawled_time() {
		return profile_crawled_time;
	}
	public String getProfile_crawled_time(String aDateFormat) {
		if(getProfile_crawled_time()==null)
			return null;
		SimpleDateFormat df = new SimpleDateFormat(aDateFormat);
		return df.format(getProfile_crawled_time());
	}
	
	public void setProfile_crawled_time(Date profile_crawled_time) {
		this.profile_crawled_time = profile_crawled_time;
	}
	public void setProfile_crawled_time(String aProfile_crawled_time, String aDateFormat) throws ParseException {
		SimpleDateFormat df = new SimpleDateFormat(aDateFormat);
		setProfile_crawled_time(df.parse(aProfile_crawled_time));
	}
	
	
	// PHOTO
	public void setPhoto_profile_cache(String photo_profile_cache) throws IOException {
		if(photo_profile_cache!=null)
			this.photo_profile_cache = photo_profile_cache.replaceAll("\\\\", "/");
		else
			this.photo_profile_cache = null;
	}
	public String getPhoto_profile_cache() {
		return photo_profile_cache;
	}	
	public void setPhoto_cover_cache(String photo_cover_cache) throws IOException {
		if(photo_cover_cache!=null)
			this.photo_cover_cache = photo_cover_cache.replaceAll("\\\\", "/");
		else
			this.photo_cover_cache = null;	
	}
	public String getPhoto_cover_cache() {
		return photo_cover_cache;
	}
	
	public String getPhoto_profile_url() {
		return photo_profile_url;
	}
	public void setPhoto_profile_url(String photo_profile_url) {
		this.photo_profile_url = photo_profile_url;
	}
	public String getPhoto_cover_url() {
		return photo_cover_url;
	}
	public void setPhoto_cover_url(String photo_cover_url) {
		this.photo_cover_url = photo_cover_url;
	}
	// Binary image
	public BufferedImage getPhoto_profile_image() {
		return photo_profile_image;
	}
	public BufferedImage getPhoto_cover_image() {
		return photo_cover_image;
	}
	
	private void setPhoto_cover_image(BufferedImage photo_cover_image) {
		this.photo_cover_image = photo_cover_image;
	}
	private void setPhoto_profile_image(BufferedImage photo_profile_image) {
		this.photo_profile_image = photo_profile_image;
	}
	
	public boolean loadPhoto_cover_image() throws IOException {	
		String sLoadImagePath = null;
		if(getPhoto_cover_cache()!=null)
		{
			sLoadImagePath = getPhoto_cover_cache();
		}
		else
		{
			sLoadImagePath = getPhoto_cover_url();
		}

		if(sLoadImagePath!=null)
		{
			setPhoto_cover_image(ImgUtil.loadImage(getUserdata_root_path(), sLoadImagePath));
		}
		
		return getPhoto_cover_image()!=null;
	}
	public boolean loadPhoto_profile_image() throws IOException {
		String sLoadImagePath = null;
		if(getPhoto_cover_cache()!=null)
		{
			sLoadImagePath = getPhoto_profile_cache();
		}
		else
		{
			sLoadImagePath = getPhoto_profile_url();
		}

		if(sLoadImagePath!=null)
		{
			setPhoto_profile_image(ImgUtil.loadImage(getUserdata_root_path(), sLoadImagePath));
		}
		return getPhoto_profile_image()!=null;
	}
	
	public String getUserdata_root_path() {
		return userdata_root_path;
	}
	public void setUserdata_root_path(String userdata_root_path) {
		this.userdata_root_path = userdata_root_path;
	}
	
	public void freeImages()
	{
		if(photo_profile_image!=null)
			photo_profile_image.flush();
		
		if(photo_cover_image!=null)
			photo_cover_image.flush();
		
		photo_profile_image = null;
		photo_cover_image = null;
	}
	
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("profile_id").append("=").append(getProfile_id());
		sb.append("\nprofile_name").append("=").append(getProfile_name());
		sb.append("\nprofile_url").append("=").append(getProfile_url());
		sb.append("\nprofile_last_updated_time").append("=").append(getProfile_last_updated_time());
		
		sb.append("\nuserdata_root_path").append("=").append(getUserdata_root_path());
		
		sb.append("\nphoto_profile_url").append("=").append(getPhoto_profile_url());
		sb.append("\nphoto_profile_cache").append("=").append(getPhoto_profile_cache());
		sb.append("\nphoto_profile_image").append("=").append(getPhoto_profile_image());
		
		sb.append("\nphoto_cover_url").append("=").append(getPhoto_cover_url());		
		sb.append("\nphoto_cover_cache").append("=").append(getPhoto_cover_cache());		
		sb.append("\nphoto_cover_image").append("=").append(getPhoto_cover_image());
		
		return sb.toString();
	}
}
