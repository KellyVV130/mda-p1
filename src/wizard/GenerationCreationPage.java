package wizard;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.CommonPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.impl.URIConverterImpl;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.obeonetwork.m2doc.genconf.GenconfUtils;
import org.obeonetwork.m2doc.util.M2DocUtils;

import plugin.Activator;

public class GenerationCreationPage extends WizardPage {

    /**
     * A dot.
     */
    private static final String DOT = ".";

    /**
     * The generation file name.
     */
    private String generationName;

    /**
     * The destination file name.
     */
    private String destinationName;

    /**
     * The validation file name.
     */
    private String validationName;

    /**
     * Tells if the generation should be launched.
     */
    private boolean launchGeneration = true;

    /**
     * The {@link M2DocNewTemplatePage}.
     */
    private TemplateSelectionPage newTemplatePage;

    /**
     * The generation name {@link Text}.
     */
    private Text generationNameText;

    /**
     * The destination name {@link Text}.
     */
    private Text destinationNameText;

    /**
     * The validation name {@link Text}.
     */
    private Text validationNameText;
    
    /**
     * the folder of the generation files in the model project folder.
     */
    private String FOLDER_NAME = "/docs/";
    
    private final EObject variableValue;

    /**
     * Constructor.
     * 
     * @param newTemplatePage
     *            the {@link M2DocNewTemplatePage}
     * @param mainVariablePage
     *            the {@link M2DocMainVariablePage}
     */
    protected GenerationCreationPage(TemplateSelectionPage newTemplatePage, EObject variableValue) {
        super("Configure generation.");
        this.newTemplatePage = newTemplatePage;
        this.variableValue = variableValue;
    }

    @Override
    public void createControl(Composite parent) {
        final Composite pageComposite = new Composite(parent, parent.getStyle());
        pageComposite.setLayout(new GridLayout(1, false));

        generationNameText = createGenerationNameComposite(pageComposite);
        destinationNameText = createDestinationNameComposite(pageComposite);
        validationNameText = createValidationNameComposite(pageComposite);

        validatePage();

        setControl(pageComposite);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            final URI mURI = variableValue.eResource().getURI();
            IPath modelPath = new Path(CommonPlugin.resolve(mURI).toFileString());
            modelPath=modelPath.removeLastSegments(1);
            String projectName = modelPath.lastSegment();
            final URI modelURI = URI.createPlatformResourceURI(projectName+FOLDER_NAME, true);
            final String templateName = new Path(newTemplatePage.getTemplateName()).removeFileExtension().lastSegment();
            if (generationName == null) {
                generationName = URI.createURI(modelURI.toPlatformString(true)+ 
                		templateName + DOT + GenconfUtils.GENCONF_EXTENSION_FILE).path();
                generationNameText.setText(generationName);
            }
            if (destinationName == null) {
                destinationName = URI.createURI(modelURI.toPlatformString(true)+ 
                		templateName + "-result." + M2DocUtils.DOCX_EXTENSION_FILE).resolve(modelURI).path();
                destinationNameText.setText(destinationName);
            }
            if (validationName == null) {
                validationName = URI.createURI(modelURI.toPlatformString(true)+ 
                		templateName + "-validation." + M2DocUtils.DOCX_EXTENSION_FILE).resolve(modelURI).path();
                validationNameText.setText(validationName);
            }
            validatePage();
        }
    }

    public String getFOLDER_NAME() {
		return FOLDER_NAME;
	}

	public void setFOLDER_NAME(String fOLDER_NAME) {
		FOLDER_NAME = fOLDER_NAME;
	}

    /**
     * Creates the generation name composite.
     * 
     * @param composite
     *            the container {@link Composite}
     * @return the created {@link Text}
     */
    private Text createGenerationNameComposite(Composite composite) {
        final Composite generationNameComposite = new Composite(composite, composite.getStyle());
        generationNameComposite.setLayout(new GridLayout(3, false));
        generationNameComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        final Label generationNameLabel = new Label(generationNameComposite, composite.getStyle());
        generationNameLabel.setText("Generation file:");
        final Text res = new Text(generationNameComposite, SWT.READ_ONLY);
        res.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        
        validatePage();

        return res;
    }

    /**
     * Creates the destination name composite.
     * 
     * @param composite
     *            the container {@link Composite}
     * @return the created {@link Text}
     */
    private Text createDestinationNameComposite(Composite composite) {
        final Composite destinationNameComposite = new Composite(composite, composite.getStyle());
        destinationNameComposite.setLayout(new GridLayout(3, false));
        destinationNameComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        final Label destinationNameLabel = new Label(destinationNameComposite, composite.getStyle());
        destinationNameLabel.setText("Result file:");
        final Text res = new Text(destinationNameComposite, SWT.READ_ONLY);
        res.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        
        validatePage();

        return res;
    }

    /**
     * Creates the validation name composite.
     * 
     * @param composite
     *            the container {@link Composite}
     * @return the created {@link Text}
     */
    private Text createValidationNameComposite(Composite composite) {
        final Composite validationNameComposite = new Composite(composite, composite.getStyle());
        validationNameComposite.setLayout(new GridLayout(3, false));
        validationNameComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        final Label validationNameLabel = new Label(validationNameComposite, composite.getStyle());
        validationNameLabel.setText("Validation file:");
        final Text res = new Text(validationNameComposite, SWT.READ_ONLY);
        res.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        
        validatePage();

        return res;
    }

    /**
     * Validates the page with the given template name.
     */
    private void validatePage() {
        if (generationName == null) {
            setErrorMessage("The generation configuration file must be defined.");
            setPageComplete(false);
        } else if (!generationName.endsWith(DOT + GenconfUtils.GENCONF_EXTENSION_FILE)) {
            setErrorMessage(String.format("The generation configuration file must ends with \".%s\".",
                    GenconfUtils.GENCONF_EXTENSION_FILE));
            setPageComplete(false);
        } else {
            if (destinationName == null) {
                setErrorMessage("The result file must be defined.");
                setPageComplete(false);
            } else if (!destinationName.endsWith(DOT + M2DocUtils.DOCX_EXTENSION_FILE)) {
                setErrorMessage(
                        String.format("The result file must ends with \".%s\".", M2DocUtils.DOCX_EXTENSION_FILE));
                setPageComplete(false);
            } else {
                if (validationName == null) {
                    setErrorMessage("The validation file must be defined.");
                    setPageComplete(false);
                } else if (!validationName.endsWith(DOT + M2DocUtils.DOCX_EXTENSION_FILE)) {
                    setErrorMessage(String.format("The validation file must ends with \".%s\".",
                            M2DocUtils.DOCX_EXTENSION_FILE));
                    setPageComplete(false);
                } else {
                    setErrorMessage(null);
                    setPageComplete(true);
                }
            }
        }
    }

    /**
     * Gets the generation file name.
     * 
     * @return the generation file name
     */
    public String getGenerationName() {
        return generationName;
    }

    /**
     * Gets the destination file name.
     * 
     * @return the destination file name
     */
    public String getDestinationName() {
        return destinationName;
    }

    /**
     * Gets the validation file name.
     * 
     * @return the validation file name
     */
    public String getValidationName() {
        return validationName;
    }
    
    /**
     * Tells if the generation should be launched.
     * 
     * @return <code>true</code> if the generation should be launched, <code>false</code> otherwise
     */
    public boolean getLaunchGeneration() {
        return launchGeneration;
    }

}
