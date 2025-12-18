package com.embabel.gekko.domain;

import com.embabel.gekko.agent.TraderAgent;

public class Analysts {

    public record FundamentalsReport(String content) implements TraderAgent.Report {
    }

    public record MarketReport(String content) implements TraderAgent.Report {
    }

    public record NewsReport(String content) implements TraderAgent.Report {
    }

    public record SocialMediaReport(String content) implements TraderAgent.Report {
    }
}
