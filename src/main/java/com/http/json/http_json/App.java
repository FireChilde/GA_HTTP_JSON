package com.http.json.http_json;

import java.io.File;

import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;

import org.apache.http.client.methods.HttpGet;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

/**
 * Hello world!
 *
 */
public class App  {
	public static void main( String[] args ) {

		// 시스템 명
		String systemNm = "CIMS";

		// 시스템id 및 토큰값은 https://ga-dev-tools.appspot.com/query-explorer/ 에서 획득, 30분단위로 토큰값은 만료됨
		String ids = "ga%3A213352261";
		String token = "ya29.a0AfH6SMA7LAJsp2xJDdVWJBwFt0R7YxPR2CI_NUe7b7eO3Mz6wty_NOWkU8pfShW1ErLOL7AKmEgYrOq3VD-_wEyDl72jQVqCGDac-7E41HicIJcTMLutkwdfCkLCBKeU6N-MUKz8fOmedpXi_Zr1KmQI1sJrvpl5Yqee";

		// idx:0, 조회 월
		// idx:1, 시작일
		// idx:2, 종료일
		List<String[]> params = new ArrayList<String[]>();
		params.add(new String[] { "04", "2020-04-01", "2020-04-30" });
		params.add(new String[] { "05", "2020-05-01", "2020-05-31" });
		params.add(new String[] { "06", "2020-06-01", "2020-06-30" });

		// 엑셀파일 저장경로
		String filePath = String.format("c:/downloads/ga_%s.xls", systemNm);

		List<GaResponse> rslt = fetchGA(ids, token, params);

		List<String> list = new ArrayList<String>();
		for(GaResponse row: rslt) {
			String url = row.getUrl().substring(0, row.getUrl().indexOf("&") == -1 ? row.getUrl().length():row.getUrl().indexOf("&"));
			if(!list.contains(url)){
				list.add(url);
			}
		}

		// 수집 대상 URL
		List<ExcelRow> filters = new ArrayList<ExcelRow>();
		for(String url: list){
			filters.add(new ExcelRow(url, new String[] { url }));
		}
//		filters.add(new ExcelRow("/bbs/list.do?db=notice&MenuCode=B00", new String[] { "/bbs/list.do?db=notice&MenuCode=B00" }));

		for(GaResponse row: rslt) {
			String url = row.getUrl();
			Map<String, Long> resCountMap = row.getCountMap();
			for(ExcelRow filter: filters) {

				Map<String, Long> excelCountMap = filter.getCountMap();

				for(String requestUrl: filter.getUrls()) {

					if(!url.contains(requestUrl)) {
						continue;
					}


					for(String key: resCountMap.keySet()) {
						Long count = excelCountMap.get(key);
						if(count == null) {
							count = 0L;
						}
						count += resCountMap.get(key);
						excelCountMap.put(key, count);
					}
				}
			}

		}


		try {

			HSSFWorkbook workbook = new HSSFWorkbook();

			int coffset = 3;


			HSSFSheet allSheet = workbook.createSheet("All");
			HSSFRow allHeader = allSheet.createRow(1);
			allHeader.createCell((short)2).setCellValue("URL");

			allSheet.setColumnWidth((short)2, (short)30000);
			allSheet.setColumnWidth((short)3, (short)2000);
			allSheet.setColumnWidth((short)4, (short)2000);
			allSheet.setColumnWidth((short)5, (short)2000);


			for(int i = 0 ; i < params.size(); i ++) {
				int columnIdx = i + coffset;
				allHeader.createCell((short)columnIdx).setCellValue(params.get(i)[0]);
			}

			int offset = 1;

			for(GaResponse res: rslt) {
				HSSFRow row = allSheet.createRow(++offset);

				HSSFCell cellUrl = row.createCell((short)2);

				cellUrl.setEncoding(HSSFWorkbook.ENCODING_UTF_16);

				cellUrl.setCellValue(res.getUrl());

				Map<String, Long> countMap = res.getCountMap();

				for(int i = 0 ; i < params.size(); i ++) {
					Long count = countMap.get(params.get(i)[0]);
					if(count == null) count = 0L;

					int columnIdx = i + coffset;

					HSSFCell cellCount = row.createCell((short)columnIdx);

					cellCount.setEncoding(HSSFWorkbook.ENCODING_UTF_16);
					cellCount.setCellValue(String.valueOf(count));
				}
			}




			HSSFSheet filterSheet = workbook.createSheet("Filter");
			filterSheet.setColumnWidth((short)1, (short)10000);
			filterSheet.setColumnWidth((short)2, (short)20000);
			filterSheet.setColumnWidth((short)3, (short)2000);
			filterSheet.setColumnWidth((short)4, (short)2000);
			filterSheet.setColumnWidth((short)5, (short)2000);


			HSSFRow header = filterSheet.createRow(1);
			header.createCell((short)1).setCellValue("NAME");
			header.createCell((short)2).setCellValue("URL");



			for(int i = 0 ; i < params.size(); i ++) {
				int columnIdx = i + coffset;
				header.createCell((short)columnIdx).setCellValue(params.get(i)[0]);
			}


			offset = 1;

			for(ExcelRow filter: filters) {
				System.out.println(filter.toString());


				HSSFRow row = filterSheet.createRow(++offset);

				HSSFCell cellName = row.createCell((short)1);
				HSSFCell cellUrls = row.createCell((short)2);



				cellName.setEncoding(HSSFWorkbook.ENCODING_UTF_16);
				cellUrls.setEncoding(HSSFWorkbook.ENCODING_UTF_16);

				cellName.setCellValue(filter.getName());
				cellUrls.setCellValue(Arrays.toString(filter.getUrls()));

				Map<String, Long> countMap = filter.getCountMap();

				for(int i = 0 ; i < params.size(); i ++) {
					Long count = countMap.get(params.get(i)[0]);
					if(count == null) count = 0L;

					int columnIdx = i + coffset;

					HSSFCell cellCount = row.createCell((short)columnIdx);

					cellCount.setEncoding(HSSFWorkbook.ENCODING_UTF_16);
					cellCount.setCellValue(String.valueOf(count));
				}
			}





			workbook.write(new FileOutputStream(new File(filePath)));

			System.out.println(filePath + " 에 저장완료");

		} catch(Exception ex) {
			ex.printStackTrace();
		}


	}


	private static List<GaResponse> fetchGA(String ids, String token, List<String[]> params) {

		List<GaResponse> rslt = new ArrayList<GaResponse>();

		try {

			Map<String, GaResponse> rsltMap = new HashMap<String, GaResponse>();
			for(String[] param: params) {

				boolean hasNextPage = true;
				int page = -1;
				int dataPerPage = 5000;


				while(hasNextPage) {

					page++;

					int startIdx = page * dataPerPage + 1;

					String requestURL = "https://www.googleapis.com/analytics/v3/data/ga?"
							+ "&metrics=ga%3Apageviews"
							+ "&dimensions=ga%3ApagePath"
							+ "&sort=-ga%3Apageviews"
							+ "&start-index=" + startIdx
							+ "&max-results=" + dataPerPage
							+ "&ids=" + ids
							+ "&start-date=" + param[1]
							+ "&end-date=" + param[2]
							+ "&access_token=" + token
//							+ "&filters=ga%3ApagePath=@/damdang_change/my_cst_info.do?MenuCode=C06&method=my_confirm_list&insu_type="
							;

					System.out.println(requestURL);

					HttpClient client = HttpClientBuilder.create().build(); // HttpClient 생성
					HttpGet getRequest = new HttpGet(requestURL); //GET 메소드 URL 생성


					HttpResponse response = client.execute(getRequest);

					//Response 출력
					if (response.getStatusLine().getStatusCode() != 200) {
						System.out.println("에러 : " + response.getStatusLine().getStatusCode());
						return null;
					}


					HttpEntity entity = response.getEntity();
					String body = EntityUtils.toString(entity, "UTF-8");


					JsonParser jparser = new JsonParser();
					JsonArray jarr = jparser.parse(body).getAsJsonObject().get("rows").getAsJsonArray();


					for(int i =0 , size = jarr.size(); i < size ; i ++) {
						JsonArray jrow = jarr.get(i).getAsJsonArray();
						String url = jrow.get(0).getAsString();
						String count = jrow.get(1).getAsString();

						GaResponse res = rsltMap.get(url);
						if(res == null) {
							res = new GaResponse();
							res.setUrl(url);
							rsltMap.put(url, res);
						}

						Map<String, Long> countMap = res.getCountMap();
						countMap.put(String.valueOf(param[0]), Long.valueOf(count));

					}

					hasNextPage = jarr.size() == dataPerPage;

					System.out.println("page " + page +", startIdx " + startIdx + ", size: " + jarr.size());
				}



			}


			for(String key: rsltMap.keySet()) {
				rslt.add(rsltMap.get(key));
			}



		} catch (Exception e){
			e.printStackTrace();
		}



		return rslt;

	}

	static class GaResponse {
		private String url;
		private Map<String, Long> countMap = new HashMap<String, Long>();



		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public Map<String, Long> getCountMap() {
			return countMap;
		}
		public void setCountMap(Map<String, Long> countMap) {
			this.countMap = countMap;
		}


	}

	static class ExcelRow {
		private String[] urls;
		private String name;
		private Map<String, Long> countMap = new HashMap<String, Long>();
		public ExcelRow(String name, String[] urls) {
			this.name = name;
			this.urls = urls;
		}

		public String[] getUrls() {
			return urls;
		}
		public void setUrls(String[] urls) {
			this.urls = urls;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Map<String, Long> getCountMap() {
			return countMap;
		}
		public void setCountMap(Map<String, Long> countMap) {
			this.countMap = countMap;
		}


		@Override
		public String toString() {
			return "ExcelRow [url=" + Arrays.toString(this.urls) + ", name=" + name + ", count=" + countMap.toString() + "]";
		}

	}

}
