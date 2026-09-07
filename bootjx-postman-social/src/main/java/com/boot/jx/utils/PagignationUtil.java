package com.boot.jx.utils;

import java.util.List;

import com.boot.jx.api.ApiPagination;
import com.boot.utils.ArgUtil;

public class PagignationUtil {

	public static ApiPagination setPagignation(long totalR ,int pageNo,String sortBy,String sortDir,int pageSize ) {
		ApiPagination pagi = new ApiPagination();
		pagi.setPageNo(pageNo);
		pagi.setTotal(totalR);
		pagi.setSortBy(sortBy);
		pagi.setSortDir(sortDir);
		pagi.setPageSize(pageSize);
		return pagi;
		
	}
	
	
	public static <T> ApiPagination setPagignationV1(List<T> list ,int pageNo,String sortBy,String sortDir,int pageSize ) {
		ApiPagination pagi = new ApiPagination();
		pagi.setPageNo(pageNo);
		if(ArgUtil.is(list)) {
			pagi.setTotal(list.size());
		}else {
			pagi.setTotal(0);
		}
		pagi.setSortBy(sortBy);
		pagi.setSortDir(sortDir);
		pagi.setPageSize(pageSize);
		return pagi;
		
	}
}
