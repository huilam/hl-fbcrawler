package hl.crawler.social.facebook;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;

import hl.common.ImgUtil;
import hl.common.PropUtil;
import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.FacebookClient.AccessToken;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.json.JsonException;
import com.restfb.json.JsonObject;
import com.restfb.types.User;

public class FBCrawler {

	public static String VERSION = "v0.95 build 2016-12-31 20:53";
	
	public static String PROP_FILE_FB_USERDATA 	= "facebook.user.properties";
	public static String PROP_FILE_FB_SCAWLER	= "fb.crawler.properties";
	
	public static String PROFILE_UPDATED_TIME_FORMAT = "yyyy-MM-dd'T'hh:mm:ssZ";

	
	private static String PROPKEY_ACCESS_TOKEN 	= "fb.graph.api.accesstoken";
	private static String PROPKEY_APPID 		= "fb.graph.api.appid";
	private static String PROPKEY_APPSECRET 	= "fb.graph.api.appsecret";
	
	private static String PROPKEY_FETCH_FIELDS 	= "fb.graph.api.fetch.fields";
	private static String PROPKEY_DOWNLOAD_PHOTO= "fb.graph.api.download.photo";
	private static final String DEF_FETCH_FIELDS = "id,name,link,cover.width(1080),picture.width(1080),updated_time";
	
	public static String PROPKEY_PROFILE_ID 	= "profile.id";
	public static String PROPKEY_PROFILE_NAME 	= "profile.name";
	public static String PROPKEY_PROFILE_URL 	= "profile.url";
	
	public static String PROPKEY_PROFILE_CRAWLED_TIME 	= "profile.crawled.time";
	public static String PROPKEY_PROFILE_UPDATED_TIME 	= "profile.last.update";
	
	public static String PROPKEY_PHOTO_OLD_PROFILE_PATH = "photo.profile.path";
	public static String PROPKEY_PHOTO_OLD_COVER_PATH	= "photo.cover.path";
	
	public static String PROPKEY_PHOTO_PROFILE_URL 		= "photo.profile.url";
	public static String PROPKEY_PHOTO_PROFILE_CACHE_PATH = "photo.profile.cache";
	public static String PROPKEY_PHOTO_COVER_URL 		= "photo.cover.url";
	public static String PROPKEY_PHOTO_COVER_CACHE_PATH = "photo.cover.cache";
	
	
	private static String JSON_FB_PROFILE_ID 		= "id";
	private static String JSON_FB_PROFILE_NAME 		= "name";
	private static String JSON_FB_PROFILE_LINK 		= "link";
	private static String JSON_FB_PROFILE_PICTURE 	= "picture";
	private static String JSON_FB_PROFILE_COVER 		= "cover";
	private static String JSON_FB_PROFILE_PICTURE_URL	= "url";
	private static String JSON_FB_PROFILE_COVER_URL 	= "source";
	
	private static String JSON_FB_PROFILE_CRAWLED_TIME	= "crawled_time";
	private static String JSON_FB_PROFILE_UPDATED_TIME 	= "updated_time";
	private static String JSON_FB_PROFILE_UPDATED_TIME_FORMAT = PROFILE_UPDATED_TIME_FORMAT;
	
	private FacebookClient FBclient 	= null;
	
	private static int FB_GRAPH_API_MAXID 	= 50;
	
	private static boolean IS_VERBOSE 		 = true;
	
	public FBCrawler(String aAccessToken)
	{
		if(aAccessToken==null || aAccessToken.trim().length()==0)
		{
			throw new RuntimeException("Facebook access token cannot be blank !");
		}
		
		FBclient = new DefaultFacebookClient(aAccessToken, Version.LATEST);
	}
	
	public FBCrawler(String aAppID, String aAppSecret)
	{
		AccessToken fbAccessToken = 
				new DefaultFacebookClient(Version.LATEST).obtainAppAccessToken(aAppID, aAppSecret);
		
		if(fbAccessToken!=null)
		{
			FBclient = new DefaultFacebookClient(fbAccessToken.getAccessToken(), Version.LATEST);
		}
	}
	
	private List<FBUser> getFBUsers(JsonObject aJsonObject, boolean isDownloadImage) throws IOException, ParseException
	{
		List<FBUser> listFBUsers = new ArrayList<FBUser>();
		Iterator<String> iterUserID = aJsonObject.keys();
		
		while(iterUserID.hasNext())
		{
			String sID = iterUserID.next();
			JsonObject json = (JsonObject) aJsonObject.get(sID);
			try{
				listFBUsers.add(getFBUser(json, isDownloadImage));
			}catch(JsonException ex)
			{
				System.out.println();
				System.err.println(sID+" - "+json.toString());
				break;
			}
		}
		return listFBUsers;
	}
	
	private static JsonObject getJsonObjFromProp(Properties aProp)
	{
		JsonObject json = new JsonObject();
		json.put(JSON_FB_PROFILE_ID, aProp.getProperty(PROPKEY_PROFILE_ID));
		json.put(JSON_FB_PROFILE_NAME, aProp.getProperty(PROPKEY_PROFILE_NAME));
		json.put(JSON_FB_PROFILE_LINK, aProp.getProperty(PROPKEY_PROFILE_URL));
		
		
		json.put(JSON_FB_PROFILE_UPDATED_TIME, aProp.getProperty(PROPKEY_PROFILE_UPDATED_TIME));
		json.put(JSON_FB_PROFILE_CRAWLED_TIME, aProp.getProperty(PROPKEY_PROFILE_CRAWLED_TIME));
		
		JsonObject jsonData = new JsonObject();
		jsonData.put(JSON_FB_PROFILE_PICTURE_URL, getProfilePhotoPath(aProp));
		JsonObject jsonPicture = new JsonObject();
		jsonPicture.put("data", jsonData);
		json.put(JSON_FB_PROFILE_PICTURE, jsonPicture);
		
		String sCoverUrl = getCoverPhotoPath(aProp);
		if(sCoverUrl!=null)
		{
			JsonObject jsonCover = new JsonObject();
			jsonCover.put(JSON_FB_PROFILE_COVER_URL, sCoverUrl);
			json.put(JSON_FB_PROFILE_COVER, jsonCover);
		}
		return json;
	}
	
	
	private static String getProfilePhotoPath(Properties aProp)
	{
		return getPhotoPath(aProp,1);
	}
	
	private static String getCoverPhotoPath(Properties aProp)
	{
		return getPhotoPath(aProp,2);
	}
	
	
	private static String getPhotoPath(Properties aProp, int aPhotoType)
	{
		String sPhotoPath = null;
		if(aProp!=null)
		{
			String sCachePath 	= null;
			String sOrgURL		= null;
		
			switch(aPhotoType)
			{
				case 1 : 
					sCachePath 	= PROPKEY_PHOTO_PROFILE_CACHE_PATH;
					sOrgURL		= PROPKEY_PHOTO_PROFILE_URL;
					break;
				case 2 : 
					sCachePath 	= PROPKEY_PHOTO_COVER_CACHE_PATH;
					sOrgURL		= PROPKEY_PHOTO_COVER_URL;
					break;
			}
			
			sPhotoPath = aProp.getProperty(sCachePath);
			
			if(sPhotoPath==null)
			{
				sPhotoPath = aProp.getProperty(sOrgURL);
			}
		}
		
		return sPhotoPath;
	}
	
	public static FBUser getFBUser(String aUserDataFolder, boolean isDownloadImage) throws IOException, ParseException
	{
		if(aUserDataFolder!=null && new File(aUserDataFolder).isDirectory())
		{
			aUserDataFolder = aUserDataFolder.replaceAll("\\\\", "/");
			if(!aUserDataFolder.endsWith("/"))
				aUserDataFolder += "/";
			
			Properties prop = PropUtil.loadProperties(aUserDataFolder+PROP_FILE_FB_USERDATA);
			
			JsonObject jsonFBUser = getJsonObjFromProp(prop);
			
//System.out.println(jsonFBUser.toString());
			return getFBUser(jsonFBUser, isDownloadImage);
		}
		
		return null;
	}
	
	public static FBUser getFBUser(JsonObject aJsonObject, boolean isDownloadImage) throws IOException
	{
		FBUser u = null;
		if(aJsonObject!=null)
		{
			Date now = new Date();

			u = new FBUser();
			u.setProfile_id((String)aJsonObject.get(JSON_FB_PROFILE_ID));
			u.setProfile_url((String)aJsonObject.get(JSON_FB_PROFILE_LINK));
			try{
				u.setProfile_name((String)aJsonObject.get(JSON_FB_PROFILE_NAME));
			}catch(JsonException ex)
			{
				u.setProfile_name("<anonymous>");
			}
			
			try{
				u.setProfile_crawled_time(
						(String)aJsonObject.get(JSON_FB_PROFILE_CRAWLED_TIME), 
						JSON_FB_PROFILE_UPDATED_TIME_FORMAT);
			}catch(Exception ex)
			{
				u.setProfile_crawled_time(now);
			}
			
			try{
				u.setProfile_last_updated_time(
						(String)aJsonObject.get(JSON_FB_PROFILE_UPDATED_TIME), 
						JSON_FB_PROFILE_UPDATED_TIME_FORMAT);
			}
			catch(ParseException ex)
			{
				u.setProfile_last_updated_time(now);
			}
			catch(JsonException ex)
			{
				//Can't get 'updated_time' if using app accesstoken, default to crawling time
				u.setProfile_last_updated_time(now);
			}
			
			
			try{
				JsonObject jsonPicture = (JsonObject) aJsonObject.get(JSON_FB_PROFILE_PICTURE);
				if(jsonPicture!=null)
				{
					/**
					 * "picture":{"data":
					 * 	{
					 *  	"is_silhouette":false,
					 * 	 	"url":"https://scontent.xx.fbcdn.net/v/t1.0-1/p50x50/13934651_10154332249546678_8379292444486798445_n.jpg?oh=4f9d62ab020e690ad0ee3a588fd15bbe&oe=58947654"
					 *  }
					 */
					jsonPicture = (JsonObject) jsonPicture.get("data");
					if(jsonPicture!=null)
					{
						String sImgURL = (String) jsonPicture.get(JSON_FB_PROFILE_PICTURE_URL);
						u.setPhoto_profile_url(sImgURL);
					}
				}
			}catch(JsonException ex)
			{
				u.setPhoto_profile_url("");
			}
			
			try{
				JsonObject jsonCover = (JsonObject) aJsonObject.get(JSON_FB_PROFILE_COVER);
				if(jsonCover!=null)
				{
					/**
					 * "cover":{"id":"10151677714476678",
					 *          "source":"https://scontent.xx.fbcdn.net/v/t1.0-9/s720x720/941318_10151677714476678_2057184304_n.jpg?oh=87778b4e905ad9e64f8b563b0493e3ae&oe=5896DDF9",
					 *          "offset_y":0}
					 */
					String sImgURL = (String) jsonCover.get(JSON_FB_PROFILE_COVER_URL);
					u.setPhoto_cover_url(sImgURL);
				}
			}catch(JsonException jsonEx)
			{
				//ignore if not found as this is optional
			}
			
			if(isDownloadImage)
			{
				u.loadPhoto_profile_image();
				u.loadPhoto_cover_image();
			}
		}
		
		return u;
	}
	
	public FBUser getPublicProfile(String aUserID, String aFetchFields, boolean isDownloadImage) throws IOException, ParseException
	{
		List<FBUser> listFBusers = getPublicProfiles(new String[]{aUserID}, aFetchFields, isDownloadImage);
		
		if(listFBusers.size()>0)
			return listFBusers.get(0);
		else
			return null;
	}
	
	public boolean testFBClient()
	{
		try{
			FBclient.fetchObject("me", User.class);
		}catch(Exception ex)
		{
			return false;
		}
		return true;
	}
	
	public List<FBUser> getPublicProfiles(List<String> aUserIDList, String aFetchFields, boolean isDownloadImage) throws IOException, ParseException
	{

		if(aUserIDList==null || aUserIDList.size()==0)
			return null;
		
		if(aFetchFields==null || aFetchFields.trim().length()==0)
			aFetchFields = DEF_FETCH_FIELDS;
		
//System.out.print("  - Fetching "+aUserIDList.size()+" profiles ... ");
		JsonObject jsonObj = FBclient.fetchObjects(aUserIDList, JsonObject.class, 
				Parameter.with("fields",aFetchFields)
				);
		List<FBUser> listFetched = getFBUsers(jsonObj, isDownloadImage);
//System.out.println(listFetched.size()+" fetched");
		return listFetched;
	}
	
	public List<FBUser> getPublicProfiles(String[] aUserIDs, String aFetchFields, boolean isDownloadImage) throws IOException, ParseException
	{
		if(aUserIDs==null)
			return null;
		
		return getPublicProfiles((List<String>) Arrays.asList(aUserIDs),aFetchFields, isDownloadImage);
	}
	
	public List<String> searchForUserIDs(String aSearchWord, boolean isExactMatch, int aResultLimit, int aOffset)
	{
		List<String> listIDs = new ArrayList<String>();
		
		Connection<JsonObject> publicSearch =
                FBclient.fetchConnection("search", JsonObject.class,
                		Parameter.with("q", aSearchWord),
                		Parameter.with("type", "user"), 
                		Parameter.with("limit",aResultLimit),
                		Parameter.with("offset",aOffset)
                		); 
		
		Iterator<JsonObject> iter = publicSearch.getData().iterator();
		while(iter.hasNext())
		{
			boolean isAddToList = true;
			
			JsonObject jsonObj = iter.next();
			if(isExactMatch)
			{
				String sName = ((String) jsonObj.get("name")).toLowerCase();
				isAddToList = (sName.indexOf(aSearchWord.toLowerCase())>-1);
			}
			
			if(isAddToList)
			{
				listIDs.add((String) jsonObj.get("id"));
				//System.out.println(jsonObj);
			}
		}
		
		return listIDs;
	}
	
	private static void setPropToSysEnv(Properties aProp, String[] aPropKeys)
	{
		for(String sPropKey : aPropKeys)
		{
			if(aProp.getProperty(sPropKey)!=null)
			{
				System.setProperty(sPropKey, aProp.getProperty(sPropKey));
			}
		}
	}
	
	
	public FBUser[] searchUser(String sSearchKeyword, boolean isExactMatch, int aResultLimit, String aFetchFields, boolean isDownloadImage) throws IOException, ParseException
	{
		List<String> listIDs = searchForUserIDs(sSearchKeyword,isExactMatch,aResultLimit,0);
		
		int iFrom 	= -FB_GRAPH_API_MAXID;
		int iTo 	= 0;
		
		List<FBUser> listAllFBUsers = new ArrayList<FBUser>();
		while(iTo<listIDs.size())
		{
			iFrom += FB_GRAPH_API_MAXID;
			iTo = iFrom+FB_GRAPH_API_MAXID-1;
			if(iTo>listIDs.size())
				iTo = listIDs.size();
			
			//System.out.println("iFrom="+iFrom+" - +iTo="+iTo);
			List<String> listSub = listIDs.subList(iFrom, iTo);
			
			List<FBUser> listfbusers = getPublicProfiles(listSub.toArray(new String[listSub.size()]), aFetchFields, isDownloadImage);			
			if(listfbusers!=null)
				listAllFBUsers.addAll(listfbusers);
		}
		
		return listAllFBUsers.toArray(new FBUser[listAllFBUsers.size()]);
	}
	
	public static FBCrawler getCrawler(Properties prop) throws Exception
	{
		
		if(prop==null)
			throw new Exception("Properties is null !");
			
		setPropToSysEnv(prop, new String[]{
				"http.proxyHost", "http.proxyPort",
				"https.proxyHost", "https.proxyPort"
				});
		
		String sAppID 		= prop.getProperty(PROPKEY_APPID);
		String sAppSecret 	= prop.getProperty(PROPKEY_APPSECRET);
		String sAccessToken = prop.getProperty(PROPKEY_ACCESS_TOKEN);

		FBCrawler fb = null;
		
		//User AccessToken take precedence 
		if (sAccessToken!=null && sAccessToken.trim().length()>0)
		{
			fb = new FBCrawler(sAccessToken);
			if(!fb.testFBClient())
			{
				fb = null;
			}
		}
		
		//App AccessToken
		if(fb==null && sAppID!=null && sAppSecret!=null)
		{
			fb = new FBCrawler(sAppID, sAppSecret);
		}
		
		if(fb==null)
		{
			throw new RuntimeException("\n[ERROR] Fail to initialize facebook RESTful API client ! "
					+"\n  - [Reason] : Invalid property value for 'fb.graph.api.accesstoken' in file ["+PROP_FILE_FB_SCAWLER+"].");
		}
		
		return fb;
	}
	
	public static boolean verifyAccessToken(FBCrawler crawler)
	{
		if(crawler!=null)
		{
			try{
				//FBclient.fetchObject("me", User.class);
			}catch(Exception ex)
			{
				return false;
			}
		}
		return true;
	}
	
	public static void unpackCuaFBUserData(FBUser[] aFbUsers, String aFbUserDataRootFolder, boolean isDownloadImage) throws IOException
	{
		if(aFbUsers==null || aFbUsers.length==0)
			return;
		
		File f = new File(aFbUserDataRootFolder);
		if(!f.exists())
			f.mkdirs();
		
		long iTotalProfiles = aFbUsers.length;
		long iCount 		= 0;
		long iUnpackCount 	= 0;
		long iSkipCount 	= 0;
		
		DecimalFormat numf = new DecimalFormat("#00.00");
		
		SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

		if(IS_VERBOSE)
		{
			System.out.println("["+df.format(new Date())+"] Unpacking data to "+aFbUserDataRootFolder+" :");
		}
		
		
		for(FBUser u : aFbUsers)
		{
			String sDownloadStatus = "[done]";
			
			if(IS_VERBOSE)
			{
				++iCount;
				double iPercent = ((iCount*1.0)/iTotalProfiles)*100.00;
				System.out.format(" - [%s%%] %s. %s (%s) ...",
						numf.format(iPercent),
						iCount,
						u.getProfile_name(),
						u.getProfile_id());
			}
			
			File folderUser = new File(f.getPath()+File.separator+u.getProfile_id());
			String sUserDataPath = folderUser.getPath()+File.separator;
			
			if(folderUser.isDirectory())
			{
				SimpleDateFormat DFMT_FB = new SimpleDateFormat(PROFILE_UPDATED_TIME_FORMAT);

				Properties propUser = PropUtil.loadProperties(sUserDataPath+PROP_FILE_FB_USERDATA);
				if(propUser!=null)
				{
					String sProfileLastUpdated = (String) propUser.get(PROPKEY_PROFILE_UPDATED_TIME);
					if(sProfileLastUpdated!=null)
					{
						//profile.last.update=2016-08-09T01\:27\:07+0800
						Date dFBLastUpdated = u.getProfile_last_updated_time();
						Date dPropLastUpdated = null;
						try {
							dPropLastUpdated = DFMT_FB.parse(sProfileLastUpdated);
							
							if(dFBLastUpdated.compareTo(dPropLastUpdated)>=0)
							{	
								if(IS_VERBOSE)
									System.out.println(" [skip]");
								iSkipCount++;
								continue;
							}
						} catch (ParseException e) {
							e.printStackTrace();
							dPropLastUpdated = null;
						}
						
						sDownloadStatus = "[updated]";
					}
				}
			}
			
			folderUser.mkdirs();
			
			if(isDownloadImage)
			{
				if(u.getPhoto_profile_image()==null)
				{
					u.loadPhoto_profile_image();
				}
				
				if(u.getPhoto_profile_image()!=null)
				{
					String sFileName = u.getProfile_id()+"_profile.jpg";
					File fileImage = new File(sUserDataPath+sFileName);
					if(fileImage.exists())
					{
						fileImage.delete();
					}
					ImageIO.write(u.getPhoto_profile_image(), "jpg", fileImage);
					//
					/**
					fileImage = new File(sUserDataPath+u.getProfile_id()+"_profile.bmp");
					ImageIO.write(u.getPhoto_profile_image(), "bmp", fileImage);
					**/
					if(u.getPhoto_profile_image()!=null)
					{
						u.setPhoto_profile_cache(sFileName);
					}
				}
				
				if(u.getPhoto_cover_image()==null)
				{
					u.loadPhoto_cover_image();
				}
				if(u.getPhoto_cover_image()!=null)
				{
					String sFileName = u.getProfile_id()+"_cover.jpg";
					File fileImage = new File(sUserDataPath+sFileName);
					if(fileImage.exists())
					{
						fileImage.delete();
					}
					
					ImageIO.write(u.getPhoto_cover_image(), "jpg", fileImage);
					//
					/**
					fileImage = new File(sUserDataPath+u.getProfile_id()+"_cover.bmp");
					ImageIO.write(u.getPhoto_cover_image(), "bmp", fileImage);
					**/
					if(u.getPhoto_cover_image()!=null)
					{
						u.setPhoto_cover_cache(sFileName);
					}
				}
			}
			
			Properties prop = getFBUserProps(u);
			PropUtil.saveProperties(prop, sUserDataPath, PROP_FILE_FB_USERDATA);

			u.freeImages();
			iUnpackCount++;
			
			if(IS_VERBOSE)
				System.out.println(" "+sDownloadStatus);
		}
		if(IS_VERBOSE)
			System.out.println("["+df.format(new Date())+"] Completed. Unpacked: "+iUnpackCount+"  Skipped:"+iSkipCount);
	}
	
	public static Properties getFBUserProps(FBUser aFBUser)
	{
		Properties prop = new Properties();
		prop.put(PROPKEY_PROFILE_ID, aFBUser.getProfile_id());
		prop.put(PROPKEY_PROFILE_NAME, aFBUser.getProfile_name());
		prop.put(PROPKEY_PROFILE_URL, aFBUser.getProfile_url());
		
		if(aFBUser.getProfile_crawled_time()!=null)
		{
			prop.put(PROPKEY_PROFILE_CRAWLED_TIME, aFBUser.getProfile_crawled_time(PROFILE_UPDATED_TIME_FORMAT));
		}
		
		if(aFBUser.getProfile_last_updated_time()!=null)
		{
			prop.put(PROPKEY_PROFILE_UPDATED_TIME, aFBUser.getProfile_last_updated_time(PROFILE_UPDATED_TIME_FORMAT));
		}
		
		if(aFBUser.getPhoto_profile_cache()!=null)
		{
			prop.put(PROPKEY_PHOTO_PROFILE_CACHE_PATH, aFBUser.getPhoto_profile_cache());
		}		
		
		if(aFBUser.getPhoto_profile_url()!=null)
		{
			prop.put(PROPKEY_PHOTO_PROFILE_URL, aFBUser.getPhoto_profile_url());
		}		
		
		if(aFBUser.getPhoto_cover_cache()!=null)
		{
			prop.put(PROPKEY_PHOTO_COVER_CACHE_PATH, aFBUser.getPhoto_cover_cache());
		}		
		
		if(aFBUser.getPhoto_cover_url()!=null)
		{
			prop.put(PROPKEY_PHOTO_COVER_URL, aFBUser.getPhoto_cover_url());
		}		

		return prop;
	}
	
	
	public List<FBUser> getNLSDemoData(String aFetchFields) throws IOException, ParseException
	{
		return getPublicProfiles(new String[] {
				 "677806677" 		//Ong Hui Lam
				,"716751443" 		//Chan Siew Cheong
				,"100011685581617"	//Yusuke Takahashi
				,"100006661082213"  //Wen Zhang
				,"100000062466299"  //Chris
				,"1481186537" 		//Raja sekar
				,"100006406659002" 	//Bill LI Lei
				,"100013983524338" 	//Sarawut Nls (Tatoo matching)
				,"1487148703" 		//Raymond Leong
				,"100002080798668" 	//Mitsue Takagi
				,"1409389250" 		//Takuya Mori
				,"100003025023956" 	//HE HuiFan
				,"654243366" 		//WeiJian
				},
				aFetchFields,
				true);
	}
	
	public static String timeToWords(long lElapseTime)
	{
		long lMSecs = lElapseTime % 1000;
		long lSecs 	= lElapseTime / 1000;
		long lMins 	= lSecs / 60;
		lSecs 		= lSecs % 60;
		return lMins+" mins "+lSecs+" secs "+lMSecs+" ms";
	}
	
	public static boolean repairProfile(File aProfileFolder) throws IOException
	{
		boolean isUpdated = false;
		
		if(aProfileFolder==null || !aProfileFolder.isDirectory())
			return false;
			
		Properties propUser = PropUtil.loadProperties(aProfileFolder.getPath(), PROP_FILE_FB_USERDATA);
		
		
		String sProfileID 		= propUser.getProperty(PROPKEY_PROFILE_ID);
		String sProfileName 	= propUser.getProperty(PROPKEY_PROFILE_NAME);
		

		FBUser u = new FBUser();
		u.setProfile_id(sProfileID);
		u.setUserdata_root_path(aProfileFolder.getPath());

		if(sProfileName!=null && sProfileName.contains("\\u"))
		{
			propUser.setProperty(PROPKEY_PROFILE_NAME, new String(sProfileName));
			isUpdated = true;
		}
		
		//Older profile format 
		String sOldProfileImgPath 	= propUser.getProperty(PROPKEY_PHOTO_OLD_PROFILE_PATH);
		if(sOldProfileImgPath!=null && 
			(sOldProfileImgPath.contains("/") || sOldProfileImgPath.contains("\\")))
		{
			boolean isFile = true;
			if(sOldProfileImgPath.toLowerCase().startsWith("http"))
			{
				isFile = false;
				u.setPhoto_profile_cache(null);
				u.setPhoto_profile_url(sOldProfileImgPath);
				if(u.loadPhoto_profile_image())
				{
					u.setPhoto_profile_cache(u.getProfile_id()+"_profile.jpg");
					sOldProfileImgPath = u.getUserdata_root_path()+"/"+u.getPhoto_profile_cache();
					ImgUtil.saveAsFile(u.getPhoto_profile_image(), "JPG", new File(sOldProfileImgPath));
					isFile = true;
				}
			}
			if(isFile)
			{
				File fProfileImage = new File(sOldProfileImgPath);
				propUser.setProperty(PROPKEY_PHOTO_PROFILE_CACHE_PATH, fProfileImage.getName());
				isUpdated = true;
			}
		}
		
		String sOldCoverImgPath 	= propUser.getProperty(PROPKEY_PHOTO_OLD_COVER_PATH);
		if(sOldCoverImgPath!=null && 
			(sOldCoverImgPath.contains("/") || sOldCoverImgPath.contains("\\")))
		{
			boolean isFile = true;
			propUser.remove(PROPKEY_PHOTO_OLD_COVER_PATH);
			
			if(sOldCoverImgPath.toLowerCase().startsWith("http"))
			{
				isFile = false;
				u.setPhoto_cover_cache(null);
				u.setPhoto_cover_url(sOldCoverImgPath);				
				propUser.setProperty(PROPKEY_PHOTO_COVER_URL, sOldCoverImgPath);
				
				if(u.loadPhoto_cover_image())
				{
					u.setPhoto_cover_cache(u.getProfile_id()+"_cover.jpg");
					sOldCoverImgPath = u.getUserdata_root_path()+"/"+u.getPhoto_cover_cache();
					ImgUtil.saveAsFile(u.getPhoto_cover_image(), "JPG", new File(sOldCoverImgPath));	
					isFile = true;
				}
			}
			
			if(isFile)
			{
				File fCoverImage = new File(sOldCoverImgPath);
				propUser.setProperty(PROPKEY_PHOTO_COVER_CACHE_PATH, fCoverImage.getName());
				isUpdated = true;
			}
		}
		
		if(isUpdated)
		{
			PropUtil.saveProperties(propUser, aProfileFolder.getPath(), PROP_FILE_FB_USERDATA);
			return true;
		}
		return false;		
	}
	
	public static void main(String args[]) throws Exception
	{
		long lStartTime = System.currentTimeMillis();
		boolean isOK = false;
		
		String sLine = "";
		String sAppVer = " NLS FBCrawler "+VERSION+" ";
		for(int i=0; i<sAppVer.length(); i++)
		{
			sLine += "-";
		}

		System.out.println(sLine);
		System.out.println(sAppVer);
		System.out.println(sLine);
		
		if(args.length>=2)
		{
			
			String sCrawlMode 		= args[0].toLowerCase();
			String sOutputFolder 	= args[1].toLowerCase().replaceAll("\\\\", "/");
			long lIdRange 			= 0;
			
			if(args.length>2)
			{
				lIdRange = Long.parseLong(args[2]);
			}
			
			if(!sOutputFolder.endsWith("/"))
			{
				sOutputFolder += "/";
			}
			
			Properties prop = PropUtil.loadProperties(PROP_FILE_FB_SCAWLER);
			List<FBUser> listFBUsers = new ArrayList<FBUser>();
			FBCrawler fbcrawler = getCrawler(prop);
			String sFetchFields = prop.getProperty(PROPKEY_FETCH_FIELDS);
			
			// 
			String sOsName = System.getProperty("os.name");
			String sExt = ((sOsName.toLowerCase().indexOf("win")>-1)?"bat":"sh");
			String sResumeFileName = sOutputFolder + "/resume-"+System.currentTimeMillis()+"."+sExt;
			if(sOsName!=null)
			{
				BufferedWriter wrtResume = null;
				try{
					new File(sOutputFolder).mkdirs();
					wrtResume = new BufferedWriter(new FileWriter(new File(sResumeFileName)));
					//
					wrtResume.write("FBCrawler."+sExt+" ");
					for(String sArg : args)
					{
						wrtResume.write(sArg+" ");
					}
				}finally
				{
					if(wrtResume!=null)
					{
						wrtResume.close();
					}
				}
			}
			//
			
			if(sCrawlMode.startsWith("demo"))
			{
				listFBUsers = fbcrawler.getNLSDemoData(sFetchFields);
				sOutputFolder += "demo";
			}
			
			else if(sCrawlMode.startsWith("repair.profile")) 
			{
				long lCount = 0;
				long lFixed = 0;
				File folderOutput = new File(sOutputFolder);
				for(File f : folderOutput.listFiles())
				{
					if(f.isDirectory())
					{
						String sStatus = "[skip]";
						lCount++;
						if(IS_VERBOSE)
						{
							System.out.print(" - "+lCount+". "+f.getName()+" ... ");
						}
						if(repairProfile(f))
						{
							sStatus = "[fixed]";
							lFixed++;
						}
						if(IS_VERBOSE)
						{
							System.out.println(sStatus);
						}
					}
				}
				isOK = true;	
				
				if(IS_VERBOSE)
				{
					System.out.println();
					System.out.print(" Total fixed profile(s) : "+lFixed+"/"+lCount);
					System.out.println();
				}
			}
			
			else if(sCrawlMode.startsWith("search.fbid"))
			{
				List<String> listIDs = new ArrayList<String>();
				String sFBId = sCrawlMode.substring("search.fbid=".length());
				long lFBIdStart = Long.parseLong(sFBId);
				
				if(IS_VERBOSE)
					System.out.print("  - Crawling facebook profile ");

				long id = lFBIdStart;
				for(; id<lFBIdStart+lIdRange; id++)
				{
					listIDs.add(String.valueOf(id));
					
					if(listIDs.size()>=FB_GRAPH_API_MAXID)
					{
						if(IS_VERBOSE)
							System.out.print(".");
						listFBUsers.addAll(
								fbcrawler.getPublicProfiles(listIDs, 
								sFetchFields, 
								false));
						listIDs.clear();
					}
					
				}
				if(IS_VERBOSE)
					System.out.println();
				
				if(listIDs.size()>0)
				{
					listFBUsers.addAll(fbcrawler.getPublicProfiles(listIDs, sFetchFields, false));
				}
				
				if(listFBUsers.size()==0)
					System.out.println(" last processed id:"+id);
				
				isOK = true;
			}
			else if(sCrawlMode.startsWith("search=") || sCrawlMode.startsWith("search.name="))
			{
				boolean isExactSearch = "search.name".equalsIgnoreCase(sCrawlMode);
				
				String sSearchKeyword = sCrawlMode.substring("search=".length());
				FBUser[] fbusers = fbcrawler.searchUser(sSearchKeyword, isExactSearch, 5000, sFetchFields, false);
				listFBUsers = Arrays.asList(fbusers);
				
				sOutputFolder += sSearchKeyword;
				
				if(isExactSearch)
					sOutputFolder += ".exact";
				
				isOK = true;
			}
			else if(sCrawlMode.startsWith("fbid="))
			{
				String sFBIds = sCrawlMode.substring("fbid=".length());
				List<String> listIDs = new ArrayList<String>();
				
				if(sFBIds.indexOf(",")>-1)
				{
					StringTokenizer tk = new StringTokenizer(sFBIds,",");
					while(tk.hasMoreTokens())
					{
						listIDs.add(tk.nextToken().trim());
					}
				}
				else
				{
					listIDs.add(sFBIds);
				}
				listFBUsers = fbcrawler.getPublicProfiles(listIDs, sFetchFields, false);
				isOK = true;
			}
			
			if(listFBUsers.size()>0 || isOK)
			{
				System.out.println();
				System.out.println("   mode	: "+sCrawlMode);
				System.out.println(" output	: "+sOutputFolder);
				System.out.println(" result	: "+listFBUsers.size());
				System.out.println();
				
				if(IS_VERBOSE)
				{
					System.out.println("  - Saving facebook data & download images ");
				}
				
				boolean isDownloadImage = PropUtil.getValueAsBoolean(prop, PROPKEY_DOWNLOAD_PHOTO, true);
				
				unpackCuaFBUserData(
						listFBUsers.toArray(new FBUser[listFBUsers.size()]), 
						sOutputFolder,
						isDownloadImage);
				
				isOK = true;
			}
			
			if(isOK)
			{
				long lElapsed = System.currentTimeMillis()-lStartTime;
				System.out.println();
				System.out.println("Total Elapsed time : "+lElapsed+"ms ("+timeToWords(lElapsed)+")");
				
				File fResume = new File(sResumeFileName);
				if(fResume.exists())
				{
					fResume.delete();
				}
			}
		}
		
		if(!isOK)
		{
			System.err.println("Invalid syntax !");
			for(String arg : args)
			{
				System.err.println("- "+arg);
			}
			
			System.out.println("Syntax : FBCrawler <mode>=<target> <output folder> [optional number]");
			System.out.println("		 mode");
			System.out.println("		 - search       : search by keyword");
			System.out.println("		 - search.fbid  : search for a range of facebook ids");
			System.out.println("		 - search.name  : search for names that contain exact keyword");
			System.out.println("		 - fbid         : search by ids (multiple id with comma separated value)");
			System.out.println("Examples : ");
			System.out.println("  FBCrawler search=NEC C:/temp/fbdata/");
			System.out.println("  FBCrawler search=\"NEC laboratories Singapore\" C:/temp/fbdata/");
			System.out.println("  FBCrawler search.name=NEC C:/temp/fbdata/");
			System.out.println("  FBCrawler search.fbid=677806677 C:/temp/fbdata/ 100");
			System.out.println("  FBCrawler fbid=677806677,716751443 C:/temp/fbdata/");
		}
	}
	
}
