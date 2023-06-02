package wizard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.obeonetwork.m2doc.services.TemplateRegistry;

import plugin.Activator;

public class TemplateSelectionPage extends WizardPage {
	
	private URI selectedTemplateURI;
	private String fileExtension;
	private String fileName;

	protected TemplateSelectionPage(String pageName, String fileExtension) {
		super(pageName);
		this.fileExtension = fileExtension;
	}
	
	/**
     * {@link Collection} {@link ITreeContentProvider}.
     * 
     * @author <a href="mailto:yvan.lussaud@obeo.fr">Yvan Lussaud</a>
     */
    public static class CollectionContentProvider extends ArrayContentProvider implements ITreeContentProvider {

        @Override
        public Object[] getChildren(Object parentElement) {
            return null;
        }

        @Override
        public Object getParent(Object element) {
            return null;
        }

        @Override
        public boolean hasChildren(Object element) {
            return false;
        }

    }

	@Override
	public void createControl(Composite parent) {
		final Composite container = new Composite(parent, parent.getStyle());
		setControl(container);
        container.setLayout(new FillLayout(SWT.HORIZONTAL | SWT.VERTICAL));
        
        final TreeViewer templatesTreeViewer = new TreeViewer(container, SWT.BORDER);
        templatesTreeViewer.setContentProvider(new CollectionContentProvider());
        templatesTreeViewer.setLabelProvider(new ColumnLabelProvider());
        final List<String> registeredTemplates = new ArrayList<>(TemplateRegistry.INSTANCE.getTemplates().keySet());
        Collections.sort(registeredTemplates);
        templatesTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
            	selectedTemplateURI = TemplateRegistry.INSTANCE.getTemplates()
            			.get(((IStructuredSelection) event.getSelection()).getFirstElement());
            	// TODO copy the template to the uml parent folder? and make fileName path.
            	fileName = new Path(getSelectedTemplateURI().toFileString()).toString();
            	// Activator.debug("in template selection page"+selectedTemplateURI);
                setPageComplete(selectedTemplateURI != null);
            }
        });
        templatesTreeViewer.setInput(registeredTemplates);
        setPageComplete(selectedTemplateURI != null);
	}
	
    public URI getSelectedTemplateURI() {
    	return this.selectedTemplateURI;
    }
    
    public String getTemplateName() {
    	return fileName;
    }

}
