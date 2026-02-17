# IntelliJ Setup Guide - Viken Chaos Monkey (Maven)

## Prerequisites
- Java 21 installed (you're using Eclipse Temurin via SDKMAN)
- IntelliJ IDEA with Spring Boot plugins installed
- Maven 3.9.x installed (via Homebrew or other package manager)

## Setup Steps

### 1. **Open the Project in IntelliJ**
   - Open IntelliJ IDEA
   - Click `File` → `Open`
   - Navigate to `/Users/kelvinchingoma/Dev/Backend/Trading/viken-chaose-mokey`
   - Click `Open` as a project

### 2. **Configure Project SDK**
   - Go to `IntelliJ IDEA` → `Preferences` → `Project: viken-chaos-monkey` → `Project Structure`
   - Set **Project SDK** to `Java 21`
   - Set **Project Language Level** to `21`
   - Click `Apply` → `OK`

### 3. **Maven Configuration (Already Done)**
   The following has been configured:
   - ✅ `pom.xml` - Maven project file with all dependencies
   - ✅ Maven Spring Boot parent POM 3.2.4
   - ✅ Java 21 source/target compatibility
   - ✅ All required plugins configured

### 4. **Run Configurations Available**
   
   Two Maven run configurations have been created:
   
   **a) Maven: Run Dev** - Run the Spring Boot application
   - Goal: `spring-boot:run`
   - Active profile: `dev`
   - Environment variable: `SPRING_PROFILES_ACTIVE=dev`
   
   **b) Maven: Build** - Build the project
   - Goals: `clean package`

### 5. **Running the Application**

   **Option A: Using IntelliJ Run Configuration (Recommended)**
   1. Click on the run configuration dropdown (top toolbar)
   2. Select `Maven: Run Dev`
   3. Click the green ▶️ Run button
   4. The application will start with the `dev` profile active

   **Option B: Using Maven in Terminal**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
   ```

   **Option C: Using Maven Panel**
   1. Open `Maven` panel on the right sidebar
   2. Expand `viken-chaos-monkey` → `Plugins` → `spring-boot`
   3. Double-click `spring-boot:run`

### 6. **Building the Project**

   **Option A: Using Run Configuration (Recommended)**
   1. Select `Maven: Build` from dropdown
   2. Click ▶️ Run

   **Option B: Using Maven Panel**
   1. Open `Maven` panel on the right
   2. Expand `viken-chaos-monkey` → `Lifecycle`
   3. Double-click `package`

   **Option C: Using Terminal**
   ```bash
   mvn clean package
   ```

### 7. **Debugging**
   1. Set breakpoints in your code (click on line number)
   2. Click the green 🐛 Debug button instead of Run
   3. Use IntelliJ's debugger to step through code

### 8. **Troubleshooting**

   **If Maven dependencies not downloading:**
   ```bash
   # Force update of snapshots
   mvn clean install -U
   ```

   **If IntelliJ doesn't recognize pom.xml:**
   - Right-click `pom.xml` → `Add as Maven Project`
   - Or: `File` → `Invalidate Caches` → `Restart`

   **If Spring profiles aren't working:**
   - Verify `application-dev.properties` exists in `src/main/resources/`
   - Check environment variables in run configuration

   **Clear Maven cache:**
   ```bash
   rm -rf ~/.m2/repository
   mvn clean install
   ```

### 9. **Project Structure**
   ```
   viken-chaos-monkey/
   ├── src/main/java/viken/chaos/monkey/
   │   ├── VikenChaosMonkeyApplication.java (Entry point)
   │   ├── common/
   │   ├── config/
   │   ├── infrastructure/
   │   └── modules/
   ├── src/main/resources/
   │   ├── application.properties
   │   └── application-dev.properties
   ├── pom.xml (Maven configuration)
   └── build.gradle.kts (Legacy - can be removed)
   ```

### 10. **Key Dependencies**
   - Spring Boot 3.2.4
   - Spring Security
   - Spring AOP
   - Kubernetes Java Client (for chaos engineering)
   - Micrometer/Prometheus (for metrics)
   - Lombok (for code generation)

## Important Notes
- ✅ **Migrated from Gradle to Maven** - Solves daemon restart issues
- ✅ **No daemon process** - Maven runs independently, won't interfere with other projects
- ✅ **Faster IDE response** - No Gradle daemon overhead
- ✅ Spring-specific options passed via environment variables
- ✅ Java 21 features fully supported

## Build Info
- **Maven version:** 3.9.12 or higher
- **Java version:** 21
- **Build time:** ~1.4 seconds
- **Output JAR:** ~63MB (target/viken-chaos-monkey-1.0.0-SNAPSHOT.jar)

## Need Help?
- Check application logs in the Run console
- Review `CHAOS_MONKEY_ENTERPRISE_PLAN.md` for feature documentation
- Review `project-setup-guideline.md` for additional setup information
- Maven docs: https://maven.apache.org/

