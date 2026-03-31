# Sentence Builder — Team 47

**CS4485 Senior Design, UTD Spring 2026**

A JavaFX application that parses Project Gutenberg plain-text files, stores word frequency data in MySQL, and generates sentences using multiple algorithms.

## Prerequisites

- JDK 17+ ([Adoptium](https://adoptium.net/))
- Maven 3.8+ ([Download](https://maven.apache.org/download.cgi))
- MySQL 8.0+ ([Installer](https://dev.mysql.com/downloads/installer/))
- IntelliJ IDEA Community Edition ([Download](https://www.jetbrains.com/idea/download/))

## Quick Start

1. **Clone the repo:**
   ```bash
   git clone https://github.com/MurtazaKhan24/Sentence-Builder-Team-47.git
   cd Sentence-Builder-Team-47
   ```

2. **Set up MySQL:**
   ```sql
   CREATE DATABASE sentence_builder;
   ```
   Then run migration scripts:
   ```bash
   mysql -u root -p sentence_builder < src/db/migrations/001_create_tables.sql
   ```

3. **Build:**
   ```bash
   mvn clean compile
   ```

4. **Run:**
   ```bash
   mvn javafx:run
   ```

5. **Test:**
   ```bash
   mvn test
   ```

## Documentation

See the [Wiki](https://github.com/MurtazaKhan24/Sentence-Builder-Team-47/wiki) for:
- [Architecture](https://github.com/MurtazaKhan24/Sentence-Builder-Team-47/wiki/Architecture)
- [Setup Guide](https://github.com/MurtazaKhan24/Sentence-Builder-Team-47/wiki/Setup-Guide)
- [API & Module Docs](https://github.com/MurtazaKhan24/Sentence-Builder-Team-47/wiki/API-and-Module-Docs)
- [Coding Standards](https://github.com/MurtazaKhan24/Sentence-Builder-Team-47/wiki/Coding-Standards)

## Team

| Member | Role |
|--------|------|
| Pranava | Project Manager |
| Faris | Architect / Tech Lead |
| James | Database |
| Murtaza | Backend |
| Zohaib | Frontend |
| Neha | QA |
