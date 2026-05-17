# Self-Control Module ProGuard Rules

# Keep all public Skill classes and interfaces
-keep public class com.jnz.wuclaw.selfcontrol.** { *; }

# Keep all classes that implement Skill interface
-keep class * implements com.jnz.wuclaw.agent.tools.Skill { *; }

# Keep SelfControlRegistry
-keep class com.jnz.wuclaw.selfcontrol.SelfControlRegistry { *; }

# Keep all Skill execute methods (reflection may be used)
-keepclassmembers class * implements com.jnz.wuclaw.agent.tools.Skill {
    public *** execute(...);
}

# Keep SkillResult
-keep class com.jnz.wuclaw.agent.tools.SkillResult { *; }

# Keep tool definition classes
-keep class com.jnz.wuclaw.providers.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
