package eu.f3rog.blade.compiler.arg;

import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import blade.Arg;
import eu.f3rog.blade.compiler.ProcessorModule;
import eu.f3rog.blade.compiler.builder.ClassManager;
import eu.f3rog.blade.compiler.util.ProcessorError;

/**
 * Class {@link ArgProcessorModule}
 *
 * @author FrantisekGazo
 */
public final class ArgProcessorModule
        implements ProcessorModule {

    @Override
    public void process(final TypeElement bladeElement) throws ProcessorError {
        ClassManager.getInstance()
                .getHelper(bladeElement)
                .tryGetModule(ArgHelperModule.class);
    }

    @Override
    public void process(final RoundEnvironment roundEnv) throws ProcessorError {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Arg.class);
        for (final Element e : elements) {
            ClassManager.getInstance()
                    .getHelper((TypeElement) e.getEnclosingElement())
                    .getModule(ArgHelperModule.class)
                    .add((VariableElement) e);
        }
    }

}
