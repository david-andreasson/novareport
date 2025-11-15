package com.novareport.reporter_service.service;

import com.novareport.reporter_service.domain.NewsItem;

import java.util.List;

public interface NewsApiProvider {
    String providerName();
    List<NewsItem> fetchLatestNews();
}
