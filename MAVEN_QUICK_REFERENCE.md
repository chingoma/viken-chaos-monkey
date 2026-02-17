# Maven Quick Commands - Viken Chaos Monkey

## Common Maven Commands

### Build & Package
```bash
# Clean build
mvn clean

# Compile
mvn compile

# Package (creates JAR)
mvn package

# Package without running tests
mvn package -DskipTests

# Full clean package
mvn clean package
```

### Running the Application
```bash
# Run with development profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Run with environment variable
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run

# Run JAR directly
java -jar target/viken-chaos-monkey-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

### Dependency Management
```bash
# Download all dependencies
mvn dependency:resolve

# Show dependency tree
mvn dependency:tree

# Update snapshots
mvn clean install -U

# Clear local repository cache
rm -rf ~/.m2/repository
mvn clean install
```

### Testing
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ClassName

# Skip tests during build
mvn clean package -DskipTests
```

### IDE Integration
```bash
# Generate IDE files
mvn idea:idea

# Reimport in IntelliJ
File → Invalidate Caches → Restart
```

## IntelliJ Tips

### Run from IDE
1. Click configuration dropdown at top
2. Select **Maven: Run Dev** or **Maven: Build**
3. Click ▶️ button

### Maven Panel
- Right-side panel shows Maven structure
- Expand and double-click any goal to run it
- Lifecycle folder contains: clean, compile, test, package, install, deploy

### Keyboard Shortcuts
- **Ctrl+Alt+M** - Extract method
- **Cmd+Shift+O** - Optimize imports
- **Cmd+B** - Go to definition
- **Ctrl+/Cmd+//** - Comment/uncomment

## Common Issues & Solutions

| Problem | Solution |
|---------|----------|
| Dependencies not downloading | `mvn clean install -U` |
| IntelliJ not recognizing pom.xml | Right-click pom.xml → "Add as Maven Project" |
| Maven daemon hangs | Press Ctrl+C, then `mvn clean` |
| Application won't start | Check `application-dev.properties` exists |
| Port already in use | Kill process or change server.port |

## Project Info
- **Build Tool:** Maven 3.9.12
- **Java Version:** 21
- **Spring Boot Version:** 3.2.4
- **Output JAR:** `target/viken-chaos-monkey-1.0.0-SNAPSHOT.jar`
- **JAR Size:** ~63 MB

## Performance Tips
1. Maven is much lighter than Gradle daemon
2. No auto-restart needed
3. Safe to have multiple projects open
4. Build completes in ~1.4 seconds

