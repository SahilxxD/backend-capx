package com.capx.stockapp.model;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class dashboardResponseModel {
	
	private Stock topStock;
	private Stock allStocksCombine;
	private Map<String, Object> industryPerfromance;
	
	public dashboardResponseModel(Stock topStock, Stock allStocksCombine, Map<String, Object> industryPerfromance) {
		super();
		this.topStock = topStock;
		this.allStocksCombine = allStocksCombine;
		this.industryPerfromance = industryPerfromance;
	}

	public dashboardResponseModel() {
		// TODO Auto-generated constructor stub
	}
	
	public Map<String, Object> getIndustryPerfromance() {
		return industryPerfromance;
	}


	public void setIndustryPerfromance(Map<String, Object> industryPerfromance) {
		this.industryPerfromance = industryPerfromance;
	}


	public Stock getTopStock() {
		return topStock;
	}
	public void setTopStock(Stock topStock) {
		this.topStock = topStock;
	}
	public Stock getAllStocksCombine() {
		return allStocksCombine;
	}
	public void setAllStocksCombine(Stock allStocksCombine) {
		this.allStocksCombine = allStocksCombine;
	}
	
	
}
