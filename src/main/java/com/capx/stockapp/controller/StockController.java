package com.capx.stockapp.controller;


import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.capx.stockapp.model.Stock;
import com.capx.stockapp.model.StockEntity;
import com.capx.stockapp.model.TransactionEntity;
import com.capx.stockapp.model.dashboardResponseModel;
import com.capx.stockapp.services.api.StockServiceInterface;
import com.capx.stockapp.services.impl.StockService;

@RestController
@CrossOrigin(origins = "*")  // Allows all origins
public class StockController {
    
    private final StockServiceInterface  stockService;

    @Autowired
    public StockController(StockServiceInterface stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/data/dashboard")
    public dashboardResponseModel getDashboardData() {
        return stockService.getDashboardData();
    }
    
    @GetMapping("/data/portfolio")
    public List<Stock> getPortfolioData() {
        return stockService.getPortfolioData();
//    	return stockService.getAllStockEntities();
    }
    
    @GetMapping("/data/history")
    public List<TransactionEntity> getTransactions() {
        return stockService.getTransactions();
//    	return stockService.getAllStockEntities();
    }
    
    // Post Mapping for adding a new stock
    @PostMapping("portfolio/add")
    public List<Stock> addStockToPortfolio(@RequestBody Map<String, Object> requestData){
    	System.out.println(requestData.get("body"));
    	return stockService.addStockToPortfolio(requestData);
    }
    
    @PostMapping("portfolio/update")
    public List<Stock> updateStockToPortfolio(@RequestBody Map<String, Object> requestData){
    	System.out.println(requestData.get("body"));
    	return stockService.updateStockData(requestData);
    }
    
//    @GetMapping("/stocks/1min")
//    public Map<String, Object> getStocks1Min() {
//        List<String> symbols = Arrays.asList("AMZN", "AAPL");
//        String range = "1d";  // 1 Day (you can adjust this as needed)
//        String interval = "1m";  // 1-minute interval
//        return stockService.getRapidApiData(symbols, range, interval);
//    }
}
