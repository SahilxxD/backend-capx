package com.capx.stockapp.services.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.capx.stockapp.model.Stock;
import com.capx.stockapp.model.dashboardResponseModel;
import com.capx.stockapp.repository.PortfolioRepository;
import com.capx.stockapp.repository.TransactionRepository;
import com.capx.stockapp.services.api.StockServiceInterface;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

import com.capx.stockapp.model.StockEntity;
import com.capx.stockapp.model.TransactionEntity;


@Service
public class StockService implements StockServiceInterface {
	@Autowired
	private  PortfolioRepository stockRepository;
	
	@Autowired
	private  TransactionRepository transactionRepository;

	@Value("${rapidapi.key}")
    	private String apiKey;
	
	
	public List<StockEntity> getAllStockEntities() {
        return stockRepository.findAll();  // Fetches all records
    }
	
	public List<TransactionEntity> getTransactions() {
        return transactionRepository.findAll();  // Fetches all records
    }

	public dashboardResponseModel getDashboardData() {
		
		try {
			//creating empty list of Stockentity
			List<StockEntity> dbData = new ArrayList<StockEntity>();
			//fetching all the stocks from portfolio database
			dbData = getAllStockEntities();
		        
			//if there is no stocks present in portfolio
		        if(dbData.isEmpty()) {
		        	dashboardResponseModel dashboardResponse = new dashboardResponseModel();
		        	return dashboardResponse;
		        };
		        
		        // Join symbols from dbData
		        String symbols = joinSymbols(dbData);
		        String range = "3mo";  // 3 Months
		        String interval = "1d";  // 1-day interval
	
		        // Get data from Rapid API
		        Map<String, Object> response = getRapidApiData(symbols, range, interval);
	
		        // Log the response for debugging (you may want to remove this in production)
		        
	
		        // Combine portfolio returns
		        Stock allStockCombine = getCombinePortfolioReturns(response, dbData);
	
		        // Get the top stock
		        Stock topStock = getTopStock(response, dbData);
	
		        // Get industry performance data
		        Map<String, Object> industryPerfromance = getIndustries(dbData);
	
		        // Create dashboard response model
		        dashboardResponseModel dashboardResponse = new dashboardResponseModel(topStock, allStockCombine, industryPerfromance);

	        return dashboardResponse;

	    } catch (Exception e) {
	        // Catch any other unexpected exceptions
	        System.err.println("Unexpected error: " + e.getMessage());
	        e.printStackTrace();
	        return null;  // Or handle accordingly
	    }
	}
	
	public List<Stock> getPortfolioData(){
		List<Stock> stockList = new ArrayList<>();
	    try {
	        // Retrieve all stock entities from the database
	        List<StockEntity> dbData = getAllStockEntities();

	        // Iterate through the database data and map it to Stock objects
	        for (StockEntity item : dbData) {
	            Stock stock = new Stock();
	            stock.setSymbol(item.getSymbol());
	            stock.setName(item.getName());
	            stock.setIndustry(item.getIndustry());
	            stock.setAvgOrderPrice((int)item.getAvgOrder());  // Ensure correct casting
	            stock.setQuantity(item.getQuantity());

	            stockList.add(stock);
	        }

	    } catch (Exception e) {
	        // Handle any exceptions that might occur during data retrieval or processing
	        System.err.println("Error occurred while fetching portfolio data: " + e.getMessage());
	        e.printStackTrace();
	    }

	    return stockList; 
	}
	
	
	public List<Stock> addStockToPortfolio(Map<String, Object> requestData) {
	    List<Stock> stockList = new ArrayList<>();
	    try {
	        // Extract the "body" from the request data
	        Map<String, Object> body = (Map<String, Object>) requestData.get("body");

	        // Validate that symbol is present and quantity is greater than 0
	        String symbol = (String) body.get("symbol");
	        Integer quantity = (Integer) body.get("quantity");

	        if (symbol == null || symbol.isEmpty()) {
	            throw new IllegalArgumentException("Symbol cannot be null or empty.");
	        }

	        if (quantity == null || quantity <= 0) {
	            throw new IllegalArgumentException("Quantity must be greater than 0.");
	        }

	        // Create a new StockEntity object and populate it
	        StockEntity newStock = new StockEntity();
	        newStock.setSymbol(symbol);
	        newStock.setName((String) body.get("name"));
	        newStock.setIndustry((String) body.get("industry"));

	        // Fetch the latest price from the Yahoo Finance API
	        int latestClosePrice = getLatestClosePriceFromRapidApi(symbol);
	        newStock.setAvgOrder(latestClosePrice);
	        newStock.setQuantity(quantity);
	        newStock.setPurchasedOn(LocalDateTime.now());

	        // Save the new stock entity to the database
	        stockRepository.save(newStock);
	        
	        addTransaction(newStock, quantity, "Buy");

	        // Retrieve and return the updated portfolio data
	        stockList = getPortfolioData();

	    } catch (IllegalArgumentException e) {
	        // Handle validation errors
	        System.err.println("Validation error: " + e.getMessage());
	        e.printStackTrace();
	        throw e;  // Optionally rethrow or return a custom response
	    } catch (Exception e) {
	        // Log any other errors and handle them
	        System.err.println("Error occurred while adding stock to portfolio: " + e.getMessage());
	        e.printStackTrace();

	        // Optionally, you can throw a custom exception to propagate the error to the controller layer
	        throw new RuntimeException("Failed to add stock to portfolio", e);
	    }

	    return stockList;  // Return the updated stock list, or an empty list in case of an error    
	}

	
	
	public List<Stock> updateStockData(Map<String, Object> requestData) {
		List<Stock> stockList = new ArrayList<>();

		try {
			// Extract the request data
			Map<String, Object> body = (Map<String, Object>) requestData.get("body");

			// Fetch the stock entity based on the symbol
			String symbol = (String) body.get("symbol");
			StockEntity existingStock = stockRepository.findBySymbol(symbol);
			String action = null;

			if (existingStock != null) {
				// Validate quantity
				Integer quantity = (Integer) body.get("quantity");
				if (quantity == null || quantity <= 0) {
					throw new IllegalArgumentException("Quantity must be greater than 0.");
				}

				// Update the quantity and avgOrder (price)
				if ("Buy".equals((String) body.get("action"))) {
					action = "Buy";
					int updatedQuantity = existingStock.getQuantity() + quantity;
					int oldOrderPrice = existingStock.getQuantity() * existingStock.getAvgOrder();

					// Fetch the latest price from Yahoo Finance API
					int newClosePrice = getLatestClosePriceFromRapidApi(symbol);
					int newOrderPrice = newClosePrice * quantity;

					int calPrice = (oldOrderPrice + newOrderPrice) / updatedQuantity;

					existingStock.setQuantity(updatedQuantity);
					existingStock.setAvgOrder(calPrice); // Assuming price is a double and converting it to int

				} else if ("Sell".equals((String) body.get("action"))) {
					action = "Sell";
					// Ensure we are not selling more than we own
					int updatedQuantity = existingStock.getQuantity() - quantity;
					if (updatedQuantity < 0) {
						throw new IllegalArgumentException("Cannot sell more than owned stock.");
					}
					else if (updatedQuantity == 0) {
						stockRepository.delete(existingStock);
						System.out.println("Stock with symbol " + symbol + " has been deleted due to zero quantity.");
						// Retrieve the updated stock list
						stockList = getPortfolioData();
						
						//add in transactions
						addTransaction(existingStock,quantity,action);
						return stockList;  // Return the updated stock list or an empty list if an error occurred
					}else {
						int oldOrderPrice = existingStock.getQuantity() * existingStock.getAvgOrder();

						// Fetch the latest price from Yahoo Finance API
						int newClosePrice = getLatestClosePriceFromRapidApi(symbol);
						int newOrderPrice = newClosePrice * quantity;

						int calPrice = (oldOrderPrice - newOrderPrice) / updatedQuantity;

						existingStock.setQuantity(updatedQuantity);
						existingStock.setAvgOrder(calPrice); // Assuming price is a double and converting it to int
					}

					
				} else {
					throw new IllegalArgumentException("Invalid action. Must be 'Buy' or 'Sell'.");
				}

				// Optionally, set the modified date/time
				existingStock.setModifiedOn(LocalDateTime.now());
				
				// Save the modified stock entity
				stockRepository.save(existingStock);
				
				//change the quantiy to the request quantity
				
				addTransaction(existingStock, quantity, action);

			} else {
				throw new IllegalArgumentException("Stock with symbol " + symbol + " not found.");
			}

			// Retrieve the updated stock list
			stockList = getPortfolioData();

		} catch (IllegalArgumentException e) {
			// Handle case where stock is not found or invalid data is provided
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();

		} catch (Exception e) {
			// General exception handling for unexpected errors
			System.err.println("Error occurred while updating stock data: " + e.getMessage());
			e.printStackTrace();
		}

		return stockList;  // Return the updated stock list or an empty list if an error occurred
	}

	private void addTransaction(StockEntity transaction, int qunatity, String action) {
	    try {
	        // Create a new TransactionEntity object
	        TransactionEntity newTransaction = new TransactionEntity();
	        newTransaction.setSymbol(transaction.getSymbol());
	        newTransaction.setAction(action);
	        newTransaction.setIndustry(transaction.getIndustry());
	        newTransaction.setName(transaction.getName());
	        newTransaction.setPrice(transaction.getAvgOrder());
	        newTransaction.setQuantity(qunatity);
	        newTransaction.setDate(LocalDateTime.now());
	        
	        // Save the new transaction to the database
	        transactionRepository.save(newTransaction);
	        
	    } catch (Exception e) {
	        // Log the exception (you can use a logger like SLF4J or System.out)
	        System.err.println("Error occurred while adding transaction: " + e.getMessage());
	        e.printStackTrace(); // This will print the full stack trace of the exception
	        // You can also rethrow the exception or handle it in other ways depending on your use case
	    }
	}

	
	private int getLatestClosePriceFromRapidApi(String symbol) {
	    try {
	        // Fetch the latest price from RapidAPI
	        Map<String, Object> responseData = getRapidApiData(symbol, "1d", "1m");

	        // Check if the response contains the symbol data
	        if (!responseData.containsKey(symbol)) {
	            throw new IllegalArgumentException("No data found for symbol: " + symbol);
	        }

	        // Extract the symbol data
	        Map<String, Object> symbolData = (Map<String, Object>) responseData.get(symbol);

	        // Check if the "close" prices are available
	        if (!symbolData.containsKey("close")) {
	            throw new IllegalArgumentException("No close prices available for symbol: " + symbol);
	        }

	        // Extract the list of close prices
	        List<Double> closePrices = (List<Double>) symbolData.get("close");

	        // Check if the list of close prices is not empty
	        if (closePrices.isEmpty()) {
	            throw new IllegalStateException("No close price data available for symbol: " + symbol);
	        }

	        // Get the last close price
	        Double lastClosePrice = closePrices.get(closePrices.size() - 1);

	        // Round the last close price
	        BigDecimal roundedClose = new BigDecimal(lastClosePrice).setScale(0, RoundingMode.HALF_UP);
	        return roundedClose.intValue();

	    } catch (IllegalArgumentException e) {
	        // Handle cases where symbol data or close prices are missing
	        System.err.println("Error: " + e.getMessage());
	        e.printStackTrace();
	        return -1; // Indicate an error (you can change this return value to indicate failure)

	    } catch (IllegalStateException e) {
	        // Handle cases where the close prices list is empty
	        System.err.println("Error: " + e.getMessage());
	        e.printStackTrace();
	        return -1; // Indicate an error

	    } catch (Exception e) {
	        // General exception handling for unexpected errors (e.g., network issues, API failures)
	        System.err.println("Error occurred while fetching close price: " + e.getMessage());
	        e.printStackTrace();
	        return -1; // Indicate an error
	    }
	}


	
	
	private static Stock getTopStock(Map<String, Object> response, List<StockEntity> dbData) {
	    try {
	        // Validate input parameters
	        if (response == null || dbData == null) {
	            throw new IllegalArgumentException("Response or dbData cannot be null.");
	        }
	        
	        // Get the stock data
	        List<Stock> stocksData = getStocksData(response, dbData);
	        
	        // If no stocks data is returned, throw an exception
	        if (stocksData == null || stocksData.isEmpty()) {
	            throw new NoSuchElementException("No stock data available to find the top stock.");
	        }
	        
	        // Find the stock with the highest change
	        Stock bestStock = findStockWithHighestChange(stocksData);
	        
	        if (bestStock == null) {
	            throw new NoSuchElementException("No best stock found.");
	        }
	        
	        return bestStock;
	    } catch (IllegalArgumentException | NoSuchElementException e) {
	        // Handle specific known exceptions
	        System.err.println("Error: " + e.getMessage());
	        e.printStackTrace();
	    } catch (Exception e) {
	        // General exception handling for unexpected errors
	        System.err.println("An unexpected error occurred while getting the top stock: " + e.getMessage());
	        e.printStackTrace();
	    }
	    
	    return null; // Return null if an error occurred
	}

	
	
	
	private static List<Stock> getStocksData(Map<String, Object> response, List<StockEntity> dbData) {
	    List<Stock> stockList = new ArrayList<>();

	    try {
	        if (response == null || dbData == null) {
	            throw new IllegalArgumentException("Response or dbData cannot be null.");
	        }

	        for (StockEntity item : dbData) {
	            String symbol = item.getSymbol();
	            Stock stock = new Stock();
	            stock.setSymbol(item.getSymbol());
	            stock.setName(item.getName());
	            stock.setIndustry(item.getIndustry());
	            stock.setAvgOrderPrice(item.getAvgOrder());
	            stock.setQuantity(item.getQuantity());

	            // Get the data for Stock from the response map
	            Map<String, Object> resData = (Map<String, Object>) response.get(symbol);
	            if (resData == null) {
	                System.err.println("No data found for symbol: " + symbol);
	                continue; // Skip to next stock if data is missing
	            }

	            List<Integer> timestamps = (List<Integer>) resData.get("timestamp");
	            List<Double> closePrices = (List<Double>) resData.get("close");

	            if (timestamps == null || closePrices == null || timestamps.size() != closePrices.size()) {
	                System.err.println("Mismatch or missing data for symbol: " + symbol);
	                continue; // Skip to next stock if data is inconsistent
	            }

	            List<List<Object>> timeAndClosePairResult = new ArrayList<>();

	            for (int i = 0; i < timestamps.size(); i++) {
	                List<Object> timeAndClosePair = new ArrayList<>();
	                timeAndClosePair.add(((long) timestamps.get(i) * 1000)); // Convert to milliseconds
	                BigDecimal roundedClose = new BigDecimal(closePrices.get(i)).setScale(0, RoundingMode.HALF_UP);
	                timeAndClosePair.add(roundedClose);
	                timeAndClosePairResult.add(timeAndClosePair);
	            }

	            stock.setClosePricesWithTimestamps(timeAndClosePairResult);

	            if (!closePrices.isEmpty()) {
	            	// Convert the last close price (Double) to BigDecimal and round it
	                BigDecimal roundedClose = new BigDecimal(closePrices.get(closePrices.size() - 1)).setScale(0, RoundingMode.HALF_UP);
	                stock.setCurrentPrice(roundedClose.intValue());
	            }

	            // Calculate and round the change percentage
	            if (item.getAvgOrder() != 0) {
	            	// Convert the last close price (Double) to BigDecimal and round it
	                BigDecimal roundedClose = new BigDecimal(closePrices.get(closePrices.size() - 1)).setScale(0, RoundingMode.HALF_UP);
	                
	                Double change = (((roundedClose.intValue()) - (double) item.getAvgOrder()) / (double) item.getAvgOrder()) * 100;
	                BigDecimal roundedChange = new BigDecimal(change).setScale(2, RoundingMode.HALF_UP);
	                stock.setChange(roundedChange.doubleValue());
	            } else {
	                stock.setChange(0.0); // Set change to 0 if average order price is 0
	            }

	            stockList.add(stock);
	        }
	    } catch (ClassCastException e) {
	        System.err.println("Error casting data: " + e.getMessage());
	        e.printStackTrace();
	    } catch (IllegalArgumentException e) {
	        System.err.println("Invalid argument: " + e.getMessage());
	        e.printStackTrace();
	    } catch (Exception e) {
	        // Catch any other unexpected exceptions
	        System.err.println("Unexpected error while processing stock data: " + e.getMessage());
	        e.printStackTrace();
	    }

	    return stockList;  // Return the list of stocks or an empty list if an error occurred
	}



	private static Stock findStockWithHighestChange(List<Stock> stockData) {
	    try {
	        if (stockData == null || stockData.isEmpty()) {
	            throw new IllegalArgumentException("Stock data list is empty or null");
	        }

	        return stockData.stream()
	                .max(Comparator.comparingDouble(Stock::getChange))
	                .orElse(null); // This returns null if the stream is empty
	    } catch (IllegalArgumentException e) {
	        System.err.println("Error: " + e.getMessage());
	        e.printStackTrace();
	        return null; // Return null if the input data is invalid or empty
	    } catch (Exception e) {
	        // Catch any other unexpected exceptions
	        System.err.println("Unexpected error while finding the stock with the highest change: " + e.getMessage());
	        e.printStackTrace();
	        return null;
	    }
	}


	private static Stock getCombinePortfolioReturns(Map<String, Object> response, List<StockEntity> dbData) {

	    Integer currentPortfolioPrice = null;
	    int totalInvested = 0;
	    int totalQuantity = 0;

	    // List to store the combined timestamp and close value pairs
	    List<List<Object>> result = new ArrayList<>();

	    try {
	        // Calculate the total invested amount
	        totalInvested = calculateTotal(dbData);
	        
	        // Calculate the total quantity
	        totalQuantity = calculateTotalQuantity(dbData);

	        // Ensure response and dbData are not empty
	        if (response == null || response.isEmpty() || dbData == null || dbData.isEmpty()) {
	            throw new IllegalArgumentException("Response data or database data is empty");
	        }

	        // Iterate over all keys in the response map
	        for (Map.Entry<String, Object> entry : response.entrySet()) {
	            String symbol = entry.getKey();
	            Map<String, Object> symbolData = (Map<String, Object>) entry.getValue();

	            // Access timestamp list for each symbol
	            List<Integer> timestamps = (List<Integer>) symbolData.get("timestamp");
	            int length = getTimestapsLength(response);

	            for (int i = 0; i < length; i++) {
	                double totalPrice = 0;

	                for (Map.Entry<String, Object> entry1 : response.entrySet()) {
	                    String symbol1 = entry1.getKey();
	                    Map<String, Object> symbolData1 = (Map<String, Object>) entry1.getValue();

	                    // Find the corresponding stock details in dbData
	                    StockEntity stockDetailMap = dbData.stream()
	                            .filter(stockDetail -> symbol1.equals(stockDetail.getSymbol()))
	                            .findFirst()
	                            .orElse(null);

	                    if (stockDetailMap != null) {
	                        int stockQuantity = (int) stockDetailMap.getQuantity();

	                        // Access closePrices list for each symbol
	                        List<Double> closePrices = (List<Double>) symbolData1.get("close");
	                        double close = closePrices.get(i);
	                     // Round close value to 2 decimal places
	    	                BigDecimal calClose = new BigDecimal(close).setScale(0, RoundingMode.HALF_UP);
	                        totalPrice += calClose.intValue() * stockQuantity;
	                    }
	                }

	                List<Object> timeAndClosePair = new ArrayList<>();
	                timeAndClosePair.add(((long) timestamps.get(i) * 1000));

	                // Round close value to 2 decimal places
	                BigDecimal roundedClose = new BigDecimal(totalPrice).setScale(0, RoundingMode.HALF_UP);
	                timeAndClosePair.add(roundedClose);
	                result.add(timeAndClosePair);

	                if (i == length - 1) {
	                    currentPortfolioPrice = roundedClose.intValue();
	                }
	            }
	            break;
	        }

	        if (currentPortfolioPrice == null) {
	            throw new IllegalStateException("Failed to calculate current portfolio price");
	        }

	        double totalChange = ((double) (currentPortfolioPrice - totalInvested) / totalInvested) * 100;

	        BigDecimal totalRoundedChange = new BigDecimal(totalChange).setScale(2, RoundingMode.HALF_UP);

	        Stock stock = new Stock();
	        stock.setSymbol("ALL");
	        stock.setName("all stocks returns");
	        stock.setAvgOrderPrice(totalInvested);
	        stock.setCurrentPrice(currentPortfolioPrice);
	        stock.setChange(totalRoundedChange.doubleValue());
	        stock.setQuantity(totalQuantity);
	        stock.setClosePricesWithTimestamps(result);

	        return stock;

	    } catch (ClassCastException e) {
	        System.err.println("Error: Data type mismatch in response: " + e.getMessage());
	        e.printStackTrace();
	    } catch (IllegalArgumentException e) {
	        System.err.println("Error: Invalid input data: " + e.getMessage());
	        e.printStackTrace();
	    } catch (IllegalStateException e) {
	        System.err.println("Error: State issue: " + e.getMessage());
	        e.printStackTrace();
	    } catch (Exception e) {
	        System.err.println("Unexpected error while calculating portfolio returns: " + e.getMessage());
	        e.printStackTrace();
	    }

	    return new Stock();  // Return an empty stock if an error occurs
	}



	private static int calculateTotal(List<StockEntity> dbData) {
	    try {
	        if (dbData == null) {
	        	return 0;
	        }

	        return (int) dbData.stream()
	                .mapToDouble(stock -> stock.getAvgOrder() * stock.getQuantity())  // Accessing avgOrder and quantity directly from StockEntity
	                .sum();  // Summing the total values
	    } catch (NullPointerException | IllegalArgumentException e) {
	        System.err.println("Error in calculating total: " + e.getMessage());
	        e.printStackTrace();
	    } catch (Exception e) {
	        System.err.println("Unexpected error in calculating total: " + e.getMessage());
	        e.printStackTrace();
	    }
	    return 0;  // Return default value if an error occurs
	}

	
	private static Map<String, Object> getIndustries(List<StockEntity> dbData) {
	    Map<String, Integer> industryQuantityMap = new HashMap<>();
	    
	    try {
	        if (dbData == null) {
	            throw new IllegalArgumentException("Database data cannot be null");
	        }

	        for (StockEntity stock : dbData) {
	            if (stock == null) continue;  // Skip if the stock entity is null
	            
	            String industry = stock.getIndustry();
	            int quantity = stock.getQuantity();

	            // Handle null industries, grouping them under "Other"
	            if (industry == null) {
	                industry = "Other";
	            }

	            // Sum quantities for each industry
	            industryQuantityMap.put(industry, industryQuantityMap.getOrDefault(industry, 0) + quantity);
	        }

	        // Prepare the result as a single object (Map)
	        Map<String, Object> result = new HashMap<>();
	        List<String> industries = new ArrayList<>();
	        List<Integer> quantities = new ArrayList<>();

	        for (Map.Entry<String, Integer> entry : industryQuantityMap.entrySet()) {
	            industries.add(entry.getKey());
	            quantities.add(entry.getValue());
	        }

	        // Adding the lists to the result map
	        result.put("industries", industries);
	        result.put("quantities", quantities);

	        // Output the result
	        return result;

	    } catch (IllegalArgumentException e) {
	        System.err.println("Error: " + e.getMessage());
	        e.printStackTrace();
	    } catch (Exception e) {
	        System.err.println("Unexpected error while calculating industry quantities: " + e.getMessage());
	        e.printStackTrace();
	    }

	    return new HashMap<>();  // Return an empty map if an error occurs
	}


	
	private static int calculateTotalQuantity(List<StockEntity> dbData) {
	    try {
	        if (dbData == null) {
	            throw new IllegalArgumentException("Database data cannot be null");
	        }

	        return dbData.stream()
	                .mapToInt(stock -> {
	                    if (stock == null) return 0;  // Return 0 if stock is null
	                    return stock.getQuantity();   // Directly accessing quantity from StockEntity
	                })
	                .sum();
	    } catch (IllegalArgumentException e) {
	        System.err.println("Error: " + e.getMessage());
	        e.printStackTrace();
	    } catch (Exception e) {
	        System.err.println("Unexpected error in calculating total quantity: " + e.getMessage());
	        e.printStackTrace();
	    }
	    return 0;  // Return default value if an error occurs
	}

	
	private static String joinSymbols(List<StockEntity> stocks) {
	    try {
	        if (stocks == null) {
	            throw new IllegalArgumentException("Stock list cannot be null");
	        }

	        return stocks.stream()
	                .map(stock -> {
	                    if (stock == null || stock.getSymbol() == null) {
	                        return "";  // Skip null or empty symbols
	                    }
	                    return stock.getSymbol();  // Get the 'symbol' field from each Stock object
	                })
	                .filter(symbol -> !symbol.isEmpty())  // Filter out empty symbols
	                .collect(Collectors.joining(","));  // Join them with a comma and a space
	    } catch (IllegalArgumentException e) {
	        System.err.println("Error: " + e.getMessage());
	        e.printStackTrace();
	    } catch (Exception e) {
	        System.err.println("Unexpected error while joining symbols: " + e.getMessage());
	        e.printStackTrace();
	    }
	    return "";  // Return an empty string if an error occurs
	}

	private static int getTimestapsLength(Map<String, Object> response) {
	    try {
	        if (response == null || response.isEmpty()) {
	            throw new IllegalArgumentException("Response map cannot be null or empty");
	        }

	        // Find the symbol with the smallest number of timestamps
	        String smallestSymbol = response.entrySet().stream()
	                .min(Comparator.comparingInt(entry -> {
	                    Map<String, Object> symbolData = (Map<String, Object>) entry.getValue();
	                    List<Long> timestamps = (List<Long>) symbolData.get("timestamp");
	                    return timestamps != null ? timestamps.size() : 0;  // Handle case where timestamps might be null
	                }))
	                .map(Map.Entry::getKey)
	                .orElse("No data");

	        // Retrieve the timestamp list size for the symbol with the smallest timestamp list
	        int smallestLength = 0;
	        if (response.containsKey(smallestSymbol)) {
	            Map<String, Object> symbolData = (Map<String, Object>) response.get(smallestSymbol);
	            List<Long> timestamps = (List<Long>) symbolData.get("timestamp");

	            if (timestamps != null) {
	                smallestLength = timestamps.size();
	            } else {
	                System.err.println("Timestamps for symbol " + smallestSymbol + " are null");
	            }
	        }

	        return smallestLength;

	    } catch (IllegalArgumentException e) {
	        System.err.println("Error: " + e.getMessage());
	        e.printStackTrace();
	    } catch (ClassCastException e) {
	        System.err.println("Error: Data structure mismatch. Ensure 'timestamp' is of type List<Long>");
	        e.printStackTrace();
	    } catch (Exception e) {
	        System.err.println("Unexpected error while calculating timestamps length: " + e.getMessage());
	        e.printStackTrace();
	    }
	    
	    return 0;  // Return 0 if an error occurs or no valid data
	}

	
	private  Map<String, Object> getRapidApiData(String symbols, String range, String interval) {
		
		String API_URL = "https://apidojo-yahoo-finance-v1.p.rapidapi.com/market/get-spark";

	    // Construct the URL with symbols, range, and interval
	    String url = API_URL + "?symbols=" + symbols + "&interval=" + interval + "&range=" + range;
	    System.out.println(url);

	    RestTemplate restTemplate = new RestTemplate();

	    // Set up headers for RapidAPI authentication
	    HttpHeaders headers = new HttpHeaders();
	    headers.set("X-RapidAPI-Key", apiKey);
	    headers.set("X-RapidAPI-Host", "apidojo-yahoo-finance-v1.p.rapidapi.com");

	    try {
	        // Make the API call
	        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, 
	                new org.springframework.http.HttpEntity<>(headers), String.class);

	        // Check for a successful response
	        if (response.getStatusCode() == HttpStatus.OK) {
	            ObjectMapper objectMapper = new ObjectMapper();
	            return objectMapper.readValue(response.getBody(), Map.class); // Parse the JSON to a Map
	        } else {
	            // Log the error response if not successful
	            throw new Exception("Failed to fetch data. Status Code: " + response.getStatusCode());
	        }
	    } catch (IOException e) {
	        // Handle JSON parsing exceptions
	        e.printStackTrace();
	        return null;
	    } catch (org.springframework.web.client.HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
	        // Handle client or server errors (4xx, 5xx)
	        System.err.println("HTTP error occurred: " + e.getStatusCode() + " " + e.getMessage());
	        return null;
	    } catch (Exception e) {
	        // Handle other unexpected errors
	        System.err.println("An error occurred: " + e.getMessage());
	        return null;
	    }
	}
	
};
