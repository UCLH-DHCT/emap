package uk.ac.ucl.rits.inform.informdb.annotation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.google.auto.service.AutoService;

/**
 * Annotation to create an audit version of a table.
 *
 * Limitations / constraints:
 * <ul>
 * <li>Static and @Transient fields are ignored
 * <li>Annotations must be on fields NOT methods
 * <li>Composite keys are not supported
 * <li>@JoinColumn & @Column cannot be used on the same field
 * <li>Primary keys must be marked with @Id
 * <li>Array types are not supported
 * <li>Nullability, name, & column definition from @JoinColumn & @Column are
 * preserved. Nothing else (eg uniqueness) is.
 * </ul>
 *
 * @author Roma Klapaukh
 *
 */
@SupportedAnnotationTypes("uk.ac.ucl.rits.inform.informdb.annotation.AuditTable")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class AuditTableProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (TypeElement annotation : annotations) {

            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

            // Must be an Entity or table. But could it be something else?

            Map<Boolean, List<TypeElement>> annotatedClasses =
                    annotatedElements.stream().map(element -> (TypeElement) element)
                            .collect(Collectors.partitioningBy(element -> element.getAnnotation(Entity.class) == null
                                    && element.getAnnotation(Table.class) == null));

            List<TypeElement> parents = annotatedClasses.get(false);
            List<TypeElement> otherClasses = annotatedClasses.get(true);

            otherClasses.forEach(element -> processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@AuditType must be applied to an @Entity or @Table class", element));

            for (TypeElement parent : parents) {
                // Create audit and main
                String packageName = null;
                String className = parent.getQualifiedName().toString();

                int lastDot = className.lastIndexOf('.');
                if (lastDot > 0) {
                    packageName = className.substring(0, lastDot);
                }

                String baseClassName = className.substring(lastDot + 1);

                try {
                    createAudit(parent, packageName, baseClassName, className);
                } catch (IOException e) {
                    e.printStackTrace();
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "@AuditType failed to be applied to " + className, parent);
                }
            }
        }

        return true;
    }

    private void createAudit(TypeElement parent, String packageName, String baseClassName, String baseImport)
            throws IOException {

        String auditClassName = baseClassName + "Audit";
        String idColumnName = baseClassName + "AuditId";
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(auditClassName);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            // Put it in a package if necessary
            if (packageName != null) {
                out.print("package ");
                out.print(packageName);
                out.println(";");
                out.println();
            }

            this.generateImports(out, baseImport);

            this.generateClassDeclaration(out, baseClassName, auditClassName);

            List<VariableElement> fields =
                    parent.getEnclosedElements().stream().filter(element -> element instanceof VariableElement)
                            .map(element -> (VariableElement) element).collect(Collectors.toList());

            List<FieldStore> shortFields = this.generateFields(out, idColumnName, fields);

            this.generateCopyConstructor(out, auditClassName, idColumnName, shortFields);

            this.generateFromMainConstructor(out, baseClassName, auditClassName, shortFields);

            // Default constructor must exist
            out.println("");
            out.println("    /**");
            out.println("     * Default constructor.");
            out.println("     */");
            out.print("    public ");
            out.print(auditClassName);
            out.println("() {}");
            out.println("");

            // Make copy method (use the copy constructor)
            out.println("    @Override ");
            out.print("    public ");
            out.print(auditClassName);
            out.println(" copy() {");
            out.print("        return new ");
            out.print(auditClassName);
            out.println("(this);");
            out.println("    }");

            // Make the createAuditEntity Method (copy constructor)
            out.println("\t@Override");
            out.print("\tpublic ");
            out.print(auditClassName);
            out.println(" createAuditEntity(Instant validUntil, Instant storedFrom) {");
            out.print("\t\t");
            out.print(auditClassName);
            out.println(" i = this.copy();");
            out.println("\t\ti.setValidUntil(validUntil);");
            out.println("\t\ti.setStoredFrom(storedFrom);");
            out.println("\t\treturn i;");
            out.println("\t}");

            out.println('}');
            out.println("");

        }
    }

    /**
     * Generate the imports.
     *
     * @param out The printWriter to write to.
     */
    private void generateImports(PrintWriter out, String baseImport) {
        out.println("import javax.persistence.Column;");
        out.println("import javax.persistence.Entity;");
        out.println("import javax.persistence.GeneratedValue;");
        out.println("import javax.persistence.GenerationType;");
        out.println("import javax.persistence.Id;");
        out.println("import javax.persistence.Inheritance;");
        out.println("import javax.persistence.InheritanceType;");
        out.println("import java.time.Instant;");
        out.println("import java.time.LocalDate;");
        out.println("import lombok.Data;");
        out.println("import lombok.EqualsAndHashCode;");
        out.println("import lombok.ToString;");
        out.println("import uk.ac.ucl.rits.inform.informdb.AuditCore;");
        out.print("import ");
        out.print(baseImport);
        out.println(';');
    }

    /**
     * Generate annotations and the class declaration for the audit class
     *
     * @param out            File to write to
     * @param baseClassName  Name of the data class
     * @param auditClassName Name of the audit class
     */
    private void generateClassDeclaration(PrintWriter out, String baseClassName, String auditClassName) {
        out.println("/**");
        out.print(" * Audit table of {@link ");
        out.print(baseClassName);
        out.println("}.");
        out.println(" */");
        out.println("@Entity");
        out.println("@Data");
        out.println("@EqualsAndHashCode(callSuper = true)");
        out.println("@ToString(callSuper = true)");
        out.println("@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)");
        out.print("public class ");
        out.print(auditClassName);
        out.print(" extends AuditCore<");
        out.print(auditClassName);
        out.println("> {");
    }

    /**
     * Generate all the fields, their getters, and setters.
     *
     * @param out        The stream to write to
     * @param primaryKey Name of the primary key for this table
     * @param fields     The fields in the object being copied.
     * @return List of fields in the original object, with the Foreign key status
     */
    private List<FieldStore> generateFields(PrintWriter out, String primaryKey, List<VariableElement> fields) {
        // Skip serialVersionUID - Different versions probably shouldn't be able to talk
        // to each other.

        List<FieldStore> fieldShorts = new ArrayList<>();

        // Primary key
        this.generateSingleField(out, "@Id\n\t@GeneratedValue(strategy = GenerationType.AUTO)", "long", primaryKey);

        // All other fields
        for (VariableElement field : fields) {
            Set<Modifier> mods = field.getModifiers();
            if (mods.contains(Modifier.STATIC) // Static field
                    || field.getAnnotation(Transient.class) != null // Transient
                    || field.getAnnotation(OneToMany.class) != null // Wrong side of a join
            ) {
                // static & transient fields are not related to the database
                continue;
            }

            // Need check to the type. If it's a primitive / Instant / String, that is fine.
            // Else it's a FK, and we need to get it's primary key to figure out the
            // linking.

            String typeName;
            TypeMirror type = field.asType();
            TypeKind kind = type.getKind();
            boolean isForeignKey =
                    field.getAnnotation(MapsId.class) != null || field.getAnnotation(JoinColumn.class) != null;
            switch (kind) {
            case LONG:
                typeName = "long";
                break;
            case SHORT:
                typeName = "short";
                break;
            case INT:
                typeName = "int";
                break;
            case FLOAT:
                typeName = "float";
                break;
            case DOUBLE:
                typeName = "double";
                break;
            case BYTE:
                typeName = "byte";
                break;
            case CHAR:
                typeName = "char";
                break;
            case DECLARED:
                DeclaredType a = (DeclaredType) type;
                TypeElement elem = (TypeElement) a.asElement();
                switch (elem.getQualifiedName().toString()) {
                case "java.lang.String":
                    typeName = "String";
                    break;
                case "java.lang.Boolean":
                    typeName = "Boolean";
                    break;
                case "java.lang.Long":
                    typeName = "Long";
                    break;
                case "java.lang.Short":
                    typeName = "Short";
                    break;
                case "java.lang.Integer":
                    typeName = "Integer";
                    break;
                case "java.lang.Float":
                    typeName = "Float";
                    break;
                case "java.lang.Double":
                    typeName = "Double";
                    break;
                case "java.lang.Byte":
                    typeName = "Byte";
                    break;
                case "java.lang.Char":
                    typeName = "Char";
                    break;
                case "java.time.LocalDate":
                    typeName = "LocalDate";
                    break;
                case "java.time.Instant":
                    typeName = "Instant";
                    break;
                default:
                    typeName = "long";
                    if (!isForeignKey) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                "Found field of type " + type + " but not detected as a foreign key");
                    }
                    continue;
                }
                break;
            default:
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Found field of unhandleable type " + type);
                continue;

            }

            String annotation = null;

            {
                Column col = field.getAnnotation(Column.class);
                if (col != null) {
                    boolean nullable = col.nullable();
                    String columnDefinition = col.columnDefinition();
                    String name = col.name();
                    annotation = String.format("@Column(columnDefinition = \"%s\", nullable=%s, name=\"%s\")",
                            columnDefinition, nullable ? "true" : "false", name);
                }
            }

            String foriegnKeyName = null;

            if (isForeignKey) {
                MapsId mapsId = field.getAnnotation(MapsId.class);
                JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);

                if (mapsId != null && joinColumn != null) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Found field with @MapsId and @JoinColumn. Cannot proceed.", field);
                    continue;
                } else if (mapsId != null) {
                    foriegnKeyName = mapsId.value();
                    if (foriegnKeyName.isBlank()) {
                        // Goes to the primary key of the type
                        // Foreign key must be a declared type - because you can't link to a primitive
                        foriegnKeyName = this.getPrimaryKeyName((DeclaredType) type);
                    }
                } else {
                    foriegnKeyName = joinColumn.referencedColumnName();
                    if (foriegnKeyName.isBlank()) {
                        // Goes to the primary key of the type
                        // Foreign key must be a declared type - because you can't link to a primitive
                        foriegnKeyName = this.getPrimaryKeyName((DeclaredType) type);
                    }
                    String colDef = joinColumn.columnDefinition();
                    boolean nullable = joinColumn.nullable();
                    String name = joinColumn.name();
                    if (annotation != null) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                "Found field has both @Column and @JoinColumn", field);
                        continue;
                    } else {
                        annotation = String.format("@Column(columnDefinition = \"%s\", nullable=%s, name=\"%s\")",
                                colDef, nullable ? "true" : "false", name);
                    }
                }
            }

            String fieldName = field.getSimpleName().toString();
            fieldShorts.add(new FieldStore(fieldName, isForeignKey, foriegnKeyName));
            this.generateSingleField(out, annotation, typeName, fieldName);
        }

        return fieldShorts;
    }

    /**
     * Generate the code for a single field declaration, its getter, and its setter.
     *
     * @param out         Stream to write to.
     * @param annotations Annotations for the field declaration.
     * @param typeName    The type of the field.
     * @param fieldName   The name of the field.
     */
    private void generateSingleField(PrintWriter out, String annotations, String typeName, String fieldName) {
        // Field declaration
        if (annotations != null) {
            out.print('\t');
            out.println(annotations);
        }
        out.print("\tprivate ");
        out.print(typeName);
        out.print(' ');
        out.print(fieldName);
        out.println(';');
        out.println();

        // Lombok will generate the getters / setters
//        // Getter
//        String nameCap = this.capitalizeInitial(fieldName);
//        out.print("\tpublic ");
//        out.print(typeName);
//        out.print(" get");
//        out.print(nameCap);
//        out.println("() {");
//        out.print("\t\treturn this.");
//        out.print(fieldName);
//        out.println(";");
//        out.println("\t}");
//        out.println();
//
//        // Setter
//        out.print("\tpublic void set");
//        out.print(nameCap);
//        out.print("(");
//        out.print(typeName);
//        out.print(' ');
//        out.print(fieldName);
//        out.println(") {");
//        out.print("\t\tthis.");
//        out.print(fieldName);
//        out.print(" = ");
//        out.print(fieldName);
//        out.println(";");
//        out.println("\t}");
//        out.println();
    }

    /**
     * Generate a copy constructor;
     *
     * @param out      File to write to
     * @param typeName The name of the class
     * @param fields   The fields that need assigning
     */
    private void generateCopyConstructor(PrintWriter out, String typeName, String primaryKey, List<FieldStore> fields) {
        out.println("\t/**");
        out.println("\t* Copy constuctor.");
        out.println("\t* @param other original entity to be copied.");
        out.println("\t*/");
        out.print("\tpublic ");
        out.print(typeName);
        out.print("(final ");
        out.print(typeName);
        out.println(" other) {");
        out.println(" super(other);");

        for (FieldStore f : fields) {
            out.print("\t\tthis.");
            out.print(f.fieldName);
            out.print(" = other.");
            out.print(f.fieldName);
            out.println(";");
        }
        out.print("\t\tthis.");
        out.print(primaryKey);
        out.print(" = other.");
        out.print(primaryKey);
        out.print(";");

        out.println("\t}");
    }

    /**
     * Generate a constructor to create an audit instance from a normal instance.
     *
     * @param out            The stream to write to.
     * @param otherClassName The name of the real instance class.
     * @param auditClassName The name of the audit instance class.
     * @param fields         The list of fields in the real instance.
     */
    private void generateFromMainConstructor(PrintWriter out, String otherClassName, String auditClassName,
            List<FieldStore> fields) {

        out.println("\t/**");
        out.println("\t* Constuctor from valid instance.");
        out.println("\t* @param other original entity to be constructred from.");
        out.println("\t* @param validUntil original entity to be constructred from.");
        out.println("\t* @param storedUntil original entity to be constructred from.");
        out.println("\t*/");
        out.print("\tpublic ");
        out.print(auditClassName);
        out.print("(final ");
        out.print(otherClassName);
        out.println(" other, Instant validUntil, Instant storedUntil) {");

        // Special handling of stored/valid until.
        out.println("\t\tsuper(validUntil, storedUntil);");
        // Ignore the primary key

        // Pull across fields from main class
        for (FieldStore f : fields) {
            out.print("\t\tthis.");
            out.print(f.fieldName);
            out.print(" = other.get");
            if (f.isForeignKey) {
                // Get the Id of foreignKeys
                out.print(capitalizeInitial(f.primaryKeyName));
            } else {
                // Ti's private to use the getter
                out.print(capitalizeInitial(f.fieldName));
            }
            out.println("();");
        }

        out.println("\t}");
    }

    /**
     * Capitalise the first letter in a String for camelCase naming
     *
     * @param s
     * @return
     */
    private String capitalizeInitial(String s) {
        String lead = s.substring(0, 1).toUpperCase();
        String tail = s.substring(1);
        return lead + tail;
    }

    /**
     * Get the name of the primary key field of a class.
     *
     * @param type The class to find the primary key for
     * @return The name of the primary key field
     */
    private String getPrimaryKeyName(DeclaredType type) {
        // Find the primary Key
        return type.asElement() // Get the class
                .getEnclosedElements() // Get all the fields and stuff
                .stream().filter(e -> e.getAnnotation(Id.class) != null).map(e -> (VariableElement) e)
                // Should only be one element, a unique default id
                .findFirst().map(e -> e.getSimpleName().toString()).get();
    }

    private class FieldStore {
        public final boolean isForeignKey;
        public final String  primaryKeyName;
        public final String  fieldName;

        public FieldStore(String fieldName, boolean isForeignKey, String primaryKeyName) {
            this.isForeignKey = isForeignKey;
            this.fieldName = fieldName;
            this.primaryKeyName = primaryKeyName;
        }
    }
}
