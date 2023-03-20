package plugin.handlers;

import java.io.File;
import java.util.Collections;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.obeonetwork.m2doc.genconf.GenconfFactory;
import org.obeonetwork.m2doc.genconf.GenconfPackage;
import org.obeonetwork.m2doc.genconf.Generation;

public class DocGeneration{
	private String path = "D:\\papyrus\\coCoME";
	Generation generation;
	DocGeneration() {
		return ;
	}
	
	public Boolean generate() {
        final URI genconfURI = getGenconfURI(new File(path));
        if (URIConverter.INSTANCE.exists(genconfURI, Collections.EMPTY_MAP)) {
            final ResourceSet rs = getResourceSetForGenconf();
            generation = getGeneration(genconfURI, rs);
        } else {
            generation = GenconfFactory.eINSTANCE.createGeneration();
            Resource r = new XMIResourceImpl(genconfURI);
            r.getContents().add(generation);
        }
        final URI templateURI = getTemplateURI(new File(path));
        setTemplateFileName(generation, URI.decode(templateURI.deresolve(genconfURI).toString()));
        final List<Exception> exceptions = new ArrayList<>();
        resourceSetForModels = getResourceSetForModel(exceptions);
        queryEnvironment = GenconfUtils.getQueryEnvironment(resourceSetForModels, generation);
        documentTemplate = M2DocUtils.parse(resourceSetForModels.getURIConverter(), templateURI, queryEnvironment,
                new ClassProvider(this.getClass().getClassLoader()), new BasicMonitor());
        for (Exception e : exceptions) {
            final XWPFRun run = M2DocUtils.getOrCreateFirstRun(documentTemplate.getDocument());
            documentTemplate.getBody().getValidationMessages()
                    .add(new TemplateValidationMessage(ValidationMessageLevel.ERROR, e.getMessage(), run));
        }
        variables = GenconfUtils.getVariables(generation, resourceSetForModels);
	}
	
	URI getGenconfURI(File testFolder) {
        return URI.createURI(testFolder.toURI().toString() + testFolder.getName() + ".genconf", false);
    }
	
	ResourceSet getResourceSetForGenconf() {
        ResourceSetImpl res = new ResourceSetImpl();

        res.getPackageRegistry().put(GenconfPackage.eNS_URI, GenconfPackage.eINSTANCE);
        res.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());

        return res;
    }
	
	Generation getGeneration(URI genconfURI, ResourceSet rs) {
        return (Generation) rs.getResource(genconfURI, true).getContents().get(0);
    }
}