# 📑 Documentation Index

## Start Here 👇

### **New to this migration?**
→ Read: **[MAVEN_CHECKLIST.md](./MAVEN_CHECKLIST.md)**

This is your quick-start guide with everything you need to know before launching the application.

---

## 📚 All Documentation Files

### **1. MAVEN_CHECKLIST.md** ⭐
- **What:** Pre-launch verification and quick-start
- **When:** Read this first!
- **Contains:**
  - Pre-launch checklist
  - Quick start steps
  - Troubleshooting
  - Project status

### **2. INTELLIJ_SETUP.md** ⭐
- **What:** Complete IntelliJ setup guide
- **When:** When setting up IntelliJ
- **Contains:**
  - Prerequisites
  - Project SDK configuration
  - Run configurations
  - Debugging setup
  - Troubleshooting

### **3. MAVEN_QUICK_REFERENCE.md**
- **What:** Cheat sheet of common Maven commands
- **When:** When you need a command quickly
- **Contains:**
  - Build commands
  - Running the app
  - Dependency management
  - Testing commands
  - Common issues

### **4. MAVEN_MIGRATION_SUMMARY.md**
- **What:** Detailed explanation of the migration
- **When:** Want to understand what changed
- **Contains:**
  - Why Maven vs Gradle
  - Current build statistics
  - What you can do now
  - Key advantages

### **5. MIGRATION_COMPLETE.md**
- **What:** Comprehensive migration summary
- **When:** Full overview needed
- **Contains:**
  - What was done
  - How to use
  - Quick stats
  - Benefits
  - Verification steps

### **6. This File (INDEX.md)**
- **What:** Navigation guide
- **When:** Finding the right documentation

---

## 🎯 Quick Navigation by Need

### "I just want to run the app"
→ [MAVEN_CHECKLIST.md](./MAVEN_CHECKLIST.md) → Quick Start section

### "How do I use IntelliJ?"
→ [INTELLIJ_SETUP.md](./INTELLIJ_SETUP.md) → Running the Application

### "What Maven commands do I need?"
→ [MAVEN_QUICK_REFERENCE.md](./MAVEN_QUICK_REFERENCE.md)

### "Why was this migrated?"
→ [MAVEN_MIGRATION_SUMMARY.md](./MAVEN_MIGRATION_SUMMARY.md) → Why Maven Instead of Gradle?

### "Tell me everything"
→ [MIGRATION_COMPLETE.md](./MIGRATION_COMPLETE.md)

---

## 📋 Quick Facts

| Item | Details |
|------|---------|
| **Build Tool** | Maven 3.9.12 (was: Gradle 8.10.2) |
| **Java Version** | 21 |
| **Spring Boot** | 3.2.4 |
| **Build Time** | ~1.4 seconds |
| **JAR Size** | 63 MB |
| **Status** | ✅ Ready to use |

---

## 🚀 30-Second Start

1. Open IntelliJ
2. Reload: `File` → `Invalidate Caches` → `Restart`
3. Click run dropdown (top toolbar)
4. Select: **"Maven: Run Dev"**
5. Click green ▶️ button
6. Done! Your app is running with dev profile

---

## 📁 Project Structure

```
viken-chaos-monkey/
├── 📄 pom.xml                                    ← Maven config
├── 📂 src/
│   └── main/
│       ├── java/viken/chaos/monkey/             ← Your code
│       └── resources/
│           ├── application.properties
│           └── application-dev.properties
├── 📂 .idea/
│   └── runConfigurations/
│       ├── Maven_Run_Dev.xml
│       └── Maven_Build.xml
└── 📄 Documentation Files:
    ├── MAVEN_CHECKLIST.md                       ← START HERE
    ├── INTELLIJ_SETUP.md
    ├── MAVEN_QUICK_REFERENCE.md
    ├── MAVEN_MIGRATION_SUMMARY.md
    ├── MIGRATION_COMPLETE.md
    └── INDEX.md (this file)
```

---

## ✅ What's Been Done

- ✅ Gradle → Maven migration complete
- ✅ All dependencies converted
- ✅ IntelliJ configurations created
- ✅ Build verified and working
- ✅ Documentation written
- ✅ Ready for development

---

## ❓ FAQ

**Q: Is my code affected?**
A: No, your source code in `src/main/java` is unchanged. Only the build system changed.

**Q: Can I still use Gradle?**
A: Yes, the Gradle files are still there. But Maven is now the primary build tool.

**Q: Do I need to install Maven?**
A: It's already installed (Maven 3.9.12 via Homebrew). But IntelliJ bundles Maven too.

**Q: Will this break my workflow?**
A: No, this should improve it! Maven is more stable with IntelliJ.

**Q: Can I use both Maven and Gradle?**
A: You can, but it's not recommended. Pick one.

---

## 🆘 Need Help?

**My issue isn't listed?**
1. Check [INTELLIJ_SETUP.md](./INTELLIJ_SETUP.md) → Troubleshooting
2. Check [MAVEN_QUICK_REFERENCE.md](./MAVEN_QUICK_REFERENCE.md) → Common Issues
3. Check [MAVEN_CHECKLIST.md](./MAVEN_CHECKLIST.md) → Pre-Launch section

**Still stuck?**
- Check Maven docs: https://maven.apache.org/
- Check Spring Boot docs: https://spring.io/projects/spring-boot
- Check IntelliJ Maven guide: https://www.jetbrains.com/help/idea/maven-support.html

---

## 📞 Support Resources

- **Official Maven Docs:** https://maven.apache.org/
- **Spring Boot Reference:** https://spring.io/projects/spring-boot
- **IntelliJ Help:** https://www.jetbrains.com/help/idea/
- **Your Project Docs:**
  - [MAVEN_QUICK_REFERENCE.md](./MAVEN_QUICK_REFERENCE.md)
  - [INTELLIJ_SETUP.md](./INTELLIJ_SETUP.md)

---

## 📊 Documentation Summary

| File | Purpose | Read Time |
|------|---------|-----------|
| MAVEN_CHECKLIST.md | Quick start & checklist | 3 min |
| INTELLIJ_SETUP.md | IntelliJ setup guide | 5 min |
| MAVEN_QUICK_REFERENCE.md | Command cheat sheet | 2 min |
| MAVEN_MIGRATION_SUMMARY.md | Why Maven? Details | 4 min |
| MIGRATION_COMPLETE.md | Full overview | 6 min |
| INDEX.md | This navigation guide | 2 min |

---

## ✨ You're All Set!

Everything is configured and ready. Pick any of the above documents based on what you need, or just follow the **"30-Second Start"** section above.

**Happy coding! 🚀**

---

**Last Updated:** February 14, 2026  
**Project:** Viken Chaos Monkey  
**Status:** ✅ Migration Complete & Verified

