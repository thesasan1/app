# Project-specific R8 rules. Libraries in use ship their own consumer rules.
# Keep kotlinx.serialization metadata for type-safe navigation routes.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
