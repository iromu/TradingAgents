package com.embabel.gekko.domain;

public class Analysts {

    public record FundamentalsReport(String content) implements ResearchTypes.Report {
    }

    public record MarketReport(String content) implements ResearchTypes.Report {
    }

    public record NewsReport(String content) implements ResearchTypes.Report {
    }

    public record SocialMediaReport(String content) implements ResearchTypes.Report {
    }
}
