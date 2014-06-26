package com.zzc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzc.core.RequestMethod;
import com.zzc.core.SimpleHttpRequest;

/**
 * 获取目标html内容。
 * 解析页面，获取字段并生成excel
 *
 */
public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);
	
	/**
	 * 每个文件记录条数
	 */
	private static final int fileSize = 1000;
	
	/**
	 * 表头名
	 */
	private static final String[] fieldName ={
		"intputWayBillNo",
		"运单号",
		"航班号",
		"日期",
		"航程",
		"货物件数",
		"货物重量",
		"货物姓名",
		"顾客id"
	};
	/**
	 * 记录选择器，每个选择器对应的字段如下
	 * 
	 *  运单号  
		航班号  
		日期    
		航程    
		货物件数
		货物重量
		货物姓名
		顾客id  
	 */
	private static final String[] cssSelects = {
		"#lblAWBNO",
		".searchData_table>table:eq(3)>tbody>tr:eq(2)>td:eq(2)",
		".searchData_table>table:eq(3)>tbody>tr:eq(2)>td:eq(3)",
		".searchData_table>table:eq(3)>tbody>tr:eq(2)>td:eq(4)",
		"#lblPcs",
		"#lblWt",
		"#lblCargoNm",
		".searchData_table>table:eq(7)>tbody>tr:eq(2)>td:eq(5)"
	};
	
	//目标url
	private static final String targetUrl = "http://huoyun.xjairport.com:8080/PublicProcess/PublicAwbQuery_URC.aspx";
	/**
	 * 线程池
	 */
	private static ExecutorService executorService = Executors.newSingleThreadExecutor();
	
	/**
	 * 主方法
	 * @param args
	 */
    public static void main(String[] args){
    	try {
    		int startNo = 0;
    		int endNo = 999999999;
    		
    		//欢迎
    		System.out.println("****************************************************************");
    		System.out.println("*                welcome,dan dan,happy everyday!               *");
    		System.out.println("*               Each file record number is "+fileSize+",               *");
    		System.out.println("*             The program will automatically split             *");
    		System.out.println("****************************************************************");
    		//读取输入
    		BufferedReader strin = new BufferedReader(new InputStreamReader(System.in));
    		while(true){
    			System.out.print("Please enter the start and end of two WayBillNo,Separated with '-' (like 5356000-5356010):");
    			String temp = strin.readLine();
    			if(!"".equals(temp)){
    				String[] inStrs = temp.split("\\-");
    				if(inStrs.length == 2){//长度为2
    					if(inStrs[0].length() == 7 && inStrs[1].length() == 7){
    						startNo = Integer.parseInt(inStrs[0]);
    						endNo = Integer.parseInt(inStrs[1]);
    						if(endNo >= startNo && startNo/1000000 > 1 && startNo/1000000 > 1){
    							break;
    						}else{
    							System.out.println("error:please check the format(like 5356000-5356010)");
    						}
    					}else{
    						System.out.println("error:Both length of 7 (like 5356000-5356010)");
    					}
    				}else{
    					System.out.println("error:Separated with '-' (like 5356000-5356010)");
    				}
    			}else{
    				System.out.println("error:can't be empty");
    			}
    		}
    		
    		//计算范围内运单号并且执行查询
    		List<List<String>> allRets = new ArrayList<>();
    		int count = 0;
    		for(int i = startNo ; i <= endNo ; i++){
    			int lastNum = i%7;
    			String wayBillNo = String.valueOf(i)+String.valueOf(lastNum);
    			logger.info("----->wayBillNo is {}.Is analyzing...",wayBillNo);
    			
    			//获取解析
    			List<String> rets = analyzePage(wayBillNo);
    			rets.add(0, wayBillNo);
    			
    			allRets.add(rets);
    			logger.info("----->Analysis of complete",wayBillNo);
    			
    			count++;
    			
    			if(count >= fileSize){//大于等于页面大小，生成文件
    				write2003Excel(allRets);//生成文件
    				allRets.clear();//清空已经生成过文件的数据
    				count = 0;//重置计数器
    			}
    		}
    		
    		//最后的文件
    		if(allRets.size() > 0){
    			write2003Excel(allRets);//生成文件
    		}
    		
    		executorService.shutdown();//停掉线程池 要不然程序不会退出
    		System.out.println("----->execute end,press any key to exit,happy everyday!");
    		strin.read();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    /**
     * 根据运单号执行查询，获取返回页面html内容
     * @param wayBillNo
     * @return
     */
    public static String search(String wayBillNo) throws Exception{
    	logger.info("----->execute query:wayBillNo is {}",wayBillNo);
    	//准备参数
    	Map<String, String> param = new HashMap<>();
    	param.put("awbtype", "AWBA");
    	param.put("AWBPRE", "880");
    	param.put("AWBNUM", wayBillNo);
    	
    	//发送请求
    	Map<String, String> requestHead = new HashMap<String, String>();
		requestHead.put("user-agent", "Mozilla/5.0 (Windows NT 6.1; rv:2.0b11) Gecko/20100101 Firefox/4.0b11");//模拟firefox
		SimpleHttpRequest simpleHttpRequest = new SimpleHttpRequest(targetUrl,RequestMethod.GET,param,requestHead);
		Future<String> future = executorService.submit(simpleHttpRequest);
		String htmlStr = "";
		try {
			htmlStr = future.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new Exception("发送请求失败，找不到页面："+e.getMessage());
		}
		
		logger.debug("----->html content:{}",htmlStr);;
    	return htmlStr;
    }

    
    /**
     * 生成文件
     * @param wayBillNo 运单号 
     * @param flightNo  航班号
     * @param flightDate 日期
     * @param flightWay  航程
     * @param cargoNum   货物件数
     * @param cargoWeight 货物重量
     * @param cargoName   货物姓名
     * @param customerId  顾客id
     */
	public void writeXls(String wayBillNo, String flightNo, String flightDate,
			String flightWay, String cargoNum, String cargoWeight,
			String cargoName, String customerId) {

	}
	
	/**
	 * 解析页面内容
	 * @param wayBillNo 运单号
	 * @return
	 */
	public static List<String> analyzePage(String wayBillNo){
		List<String> strs = new LinkedList<>();
		
		String pageContent = "";
		try {
			pageContent = search(wayBillNo);
			Document doc = Jsoup.parse(pageContent);
			for(int i = 0 ; i < cssSelects.length ; i++){
				String temp = doc.select(cssSelects[i]).text();
				logger.info("----->analyze page {}:{}",i,temp);
				if(i == 0 && temp.trim().length() == 0){//判断运单号是否为空,空说明当前运单不存在
					logger.info("----->wayBill is not found");
					strs.add("wayBill is not found!");
					break;
				}
				strs.add(temp.trim());
			}
		} catch (Exception e) {
			strs.add(e.getMessage());
		}
		
		return strs;
	}
	
	
	/**
	 * 生成03版excel
	 * @param list 数据
	 */
	public static void write2003Excel(List<List<String>> lists) throws Exception{
	    // 创建Excel的工作书册 Workbook,对应到一个excel文档
	    HSSFWorkbook wb = new HSSFWorkbook();
	    // 创建Excel的工作sheet,对应到一个excel文档的tab
	    HSSFSheet sheet = wb.createSheet("sheet1");

	    //写表头
	    HSSFRow row0 = sheet.createRow(0);
	    for(int i = 0 ; i < fieldName.length ; i++){
	    	//创建单元格
    		HSSFCell cell = row0.createCell(i);
    		cell.setCellValue(fieldName[i]);
	    }
	    
	    //写入数据
	    for(int i = 0 ; i < lists.size() ; i++){
	    	// 创建Excel的sheet的一行
	    	HSSFRow row = sheet.createRow(i+1);
	    	
	    	for(int j = 0 ; j < lists.get(i).size() ; j++){
	    		//创建单元格
	    		HSSFCell cell = row.createCell(j);
	    		cell.setCellValue(lists.get(i).get(j));
	    	}
	    }
	    
	    String path = System.getProperty("user.dir")+File.separatorChar+System.currentTimeMillis()+".xls";
	    logger.info("----->output path is :{}",path);
	    FileOutputStream os = new FileOutputStream(path);
	    wb.write(os);
	    os.flush();
	    os.close();
	}
}
