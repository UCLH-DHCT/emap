package uk.ac.ucl.rits.inform.informdb.annotation;

import java.io.IOException;
import java.io.PrintWriter;
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
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.google.auto.service.AutoService;

@SupportedAnnotationTypes("uk.ac.ucl.rits.inform.informdb.annotation.AuditTable")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class AuditTableProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (TypeElement annotation : annotations) {

            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

            Map<Boolean, List<TypeElement>> annotatedClasses = annotatedElements.stream()
                    .map(element -> (TypeElement) element)
                    .collect(Collectors.partitioningBy(
                            element -> element.getSimpleName().toString().endsWith("Parent")));

            List<TypeElement> parents = annotatedClasses.get(true);
            List<TypeElement> otherClasses = annotatedClasses.get(false);

            otherClasses.forEach(element -> processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@AuditType must be applied to a class ending in Parent", element));

            if (parents.isEmpty()) {
                continue;
            }


            for(TypeElement parent: parents) {
                // Create audit and main
                String packageName = null;
                String className = parent.getQualifiedName().toString();

                int lastDot = className.lastIndexOf('.');
                if (lastDot > 0) {
                    packageName = className.substring(0, lastDot);
                }

                String baseClassName = className.substring(lastDot + 1, className.length() - "Parent".length());

                try {
                    createAudit(parent, packageName, baseClassName);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                createMain(parent, packageName, baseClassName);
            }
        }

        return true;
    }

    private void createAudit(TypeElement parent, String packageName, String baseClassName) throws IOException {

        String auditClassName = baseClassName + "Audit";
        String idColumnName = baseClassName+ "AuditId";
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(auditClassName);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            // Put it in a package if necessary
            if (packageName != null) {
                out.print("package ");
                out.print(packageName);
                out.println(";");
                out.println();
            }

            // Add in imports
            out.println("import lombok.Data;");
            out.println("import lombok.EqualsAndHashCode;");
            out.println("import lombok.ToString;");
            out.println("import uk.ac.ucl.rits.inform.informdb.AuditCore;");
            out.println("import javax.persistence.Column;");
            out.println("import javax.persistence.Entity;");
            out.println("import javax.persistence.GeneratedValue;");
            out.println("import javax.persistence.GenerationType;");
            out.println("import javax.persistence.Id;");
            out.println("import java.time.Instant;");
            out.println();

            // Headings / Annotations of the class
            out.println("/**");
            out.print(" * Audit table of {@link ");
            out.print(baseClassName);
            out.println("}.");
            out.println(" */");
            out.println("@Entity");
            out.println("@Data");
            out.println("@EqualsAndHashCode(callSuper = true)");
            out.println("@ToString(callSuper = true)");
            out.print("public class ");
            out.print(auditClassName);
            out.print("extends ");
            out.print(baseClassName);
            out.print("Parent implements AuditCore<");
            out.print(baseClassName);
            out.println("Parent> {");

            // Class fields

            // Skip serialVersionUID - Different versions probably shouldn't be able to talk to each other.
            out.println("    @Column(columnDefinition = \"timestamp with time zone\")");
            out.println("    private Instant validUntil;");
            out.println("    @Column(columnDefinition = \"timestamp with time zone\")");
            out.println("    private Instant storedUntil;");

            // Main Id
            out.println("    @Column(nullable = false)");
            out.print("    private long ");
            out.print(baseClassName);
            out.println("Id;");
            out.println("    @Id");
            out.println("    @GeneratedValue(strategy = GenerationType.AUTO)");
            out.print("    private long ");
            out.print(idColumnName);
            out.println(";");

            // Extra fields that mismatch in the main / audit tables.
            // TODO FIX ME
            out.println("    @Column(nullable = false)");
            out.println("    private String encounter;");
            out.println("");

            // Default constructor must exist
            out.println("");
            out.println("    /**");
            out.println("     * Default constructor.");
            out.println("     */");
            out.print("    public ");
            out.print(auditClassName);
            out.println("() {}");
            out.println("");

            // Make from main constructor
            out.println("    /**");
            out.println("     * Constructor from original entity and invalidation times.");
            out.println("     * @param originalEntity original entity to be audited.");
            out.println("     * @param storedUntil    the time that this change is being made in the DB");
            out.println("     * @param validUntil     the time at which this fact stopped being true,");
            out.println("     *                       can be any amount of time in the past");
            out.println("     */");
            out.print("    public ");
            out.print(auditClassName);
            out.print("(final ");
            out.print(baseClassName);
            out.println(" originalEntity, final Instant validUntil, final Instant storedUntil) {");
            out.println("        super(originalEntity);");
            out.println("        this.validUntil = validUntil;");
            out.println("        this.storedUntil = storedUntil;");
            // Main id
            out.print("        this.");
            out.print(baseClassName);
            out.print("Id = originalEntity.get");
            out.print(baseClassName);
            out.println("Id();");
            // All other variables that are new
            // TODO fix me
            out.println("    }");

            // Make copy constructor
            out.println("    /**");
            out.println("     * Copy constuctor.");
            out.println("     * @param other original entity to be copied.");
            out.println("     */");
            out.print("    public ");
            out.print(auditClassName);
            out.print("(final ");
            out.print(auditClassName);
            out.println(" other) {");
            out.println("        super(other);");
            out.println("        this.validUntil = other.validUntil;");
            out.println("        this.storedUntil = other.storedUntil;");
            // Main id
            out.print("        this.");
            out.print(baseClassName);
            out.print("Id = other.get");
            out.print(baseClassName);
            out.println("Id();");
            // primary key
            out.print("        this.");
            out.print(idColumnName);
            out.print(" = other.get");
            out.print(idColumnName);
            out.println("();");
            // All other variables that are new
            // TODO fix me
            out.println("    }");

            // Make copy method (use the copy constructor)
            out.print("    public ");
            out.print(auditClassName);
            out.println(" copy() {");
            out.print("        return new ");
            out.print(auditClassName);
            out.println("(this);");
            out.println("    }");


            out.println("}");
            out.println("");



        }
    }

    private void createMain(TypeElement parent, String packageName, String baseClassName) {}
}
