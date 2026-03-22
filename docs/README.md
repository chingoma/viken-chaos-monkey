# Viken Chaos Monkey Documentation

Welcome to the Viken Chaos Monkey documentation. This directory contains comprehensive documentation for setting up, developing, and operating the Viken Chaos Monkey platform.

## 📚 Documentation Structure

### 🚀 Setup
Configuration and environment setup guides.
- [IntelliJ IDEA Setup](setup/INTELLIJ_SETUP.md) - IDE configuration and setup instructions
- [Project Setup Guidelines](setup/project-setup-guideline.md) - Complete project setup guide

### 🔄 Migration
Documentation related to the Gradle to Maven migration.
- [Gradle Removal Guide](migration/GRADLE_REMOVAL.md) - Steps for removing Gradle
- [Gradle Removal Complete](migration/GRADLE_REMOVAL_COMPLETE.md) - Completion status
- [Migration Complete](migration/MIGRATION_COMPLETE.md) - Overall migration status
- [Maven Migration Summary](migration/MAVEN_MIGRATION_SUMMARY.md) - Summary of Maven migration
- [Maven Checklist](migration/MAVEN_CHECKLIST.md) - Migration checklist
- [Maven Quick Reference](migration/MAVEN_QUICK_REFERENCE.md) - Quick reference for Maven commands

### 📖 Guides
User and developer guides.
- [Viken Chaos Monkey Guide](guides/viken_chaos_monkey_pdf_ready.md) - Comprehensive platform guide

### 📋 Planning
Project planning and roadmap documentation.
- [Enterprise Plan](planning/CHAOS_MONKEY_ENTERPRISE_PLAN.md) - Enterprise feature roadmap

### 📈 Observability
Production observability standards, dashboard strategy, and alerting guidance.
- [Grafana Dashboard Strategy](observability/GRAFANA_DASHBOARD_STRATEGY.md) - Full dashboard model and rollout baseline
- [Telemetry Contract](observability/TELEMETRY_CONTRACT.md) - Metrics/logs/traces standards and correlation
- [Service Dashboard Template](observability/SERVICE_DASHBOARD_TEMPLATE.md) - Reusable L3 service dashboard standard
- [Runtime and Dependency Dashboards](observability/RUNTIME_AND_DEPENDENCY_DASHBOARDS.md) - K8s, JVM, DB, cache, messaging views
- [SLO and Alerting Standard](observability/SLO_AND_ALERTING_STANDARD.md) - SLI/SLO/error-budget and alert routing
- [Release and Business Visibility](observability/RELEASE_AND_BUSINESS_VISIBILITY.md) - Deployment impact and critical journey design
- [PromQL Library](observability/PROMQL_LIBRARY.md) - Production query examples for key panels
- [Dashboard Catalog](observability/DASHBOARD_CATALOG.yaml) - Machine-readable dashboard inventory

### 📑 Index
- [Documentation Index](INDEX.md) - Quick reference index of all documentation

## 🔗 Quick Links

- [Main README](../README.md) - Project overview and quick start
- [Contributing Guidelines](../CONTRIBUTING.md) - How to contribute (if exists)
- [License](../LICENSE) - Project license (if exists)

## 📝 Documentation Standards

All documentation in this repository should follow these guidelines:
- Use Markdown format (.md)
- Include clear headings and table of contents for longer documents
- Keep language clear and concise
- Include code examples where applicable
- Update the index when adding new documentation

## 🆘 Need Help?

If you can't find what you're looking for:
1. Check the [Documentation Index](INDEX.md)
2. Search the docs directory
3. Refer to the main [README](../README.md)
4. Contact the development team

---
Last updated: February 17, 2026

