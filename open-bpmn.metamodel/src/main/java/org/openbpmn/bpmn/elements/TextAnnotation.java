package org.openbpmn.bpmn.elements;

import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.BPMNNS;
import org.openbpmn.bpmn.elements.core.BPMNElementNode;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.w3c.dom.Element;

/**
 * Text Annotations are a mechanism for a modeler to provide additional
 * information for the reader of a BPMN Diagram. A Text Annotation is an open
 * rectangle that MUST be drawn with a solid single line.
 * <p>
 * A Text Annotation object can be connected to a specific object on the Diagram
 * with an Association, but does not affect the flow
 * 
 * 
 * @author rsoika
 *
 */
public class TextAnnotation extends BPMNElementNode {

    public final static double DEFAULT_WIDTH = 75.0;
    public final static double DEFAULT_HEIGHT = 50.0;
    public static final double LABEL_OFFSET = 0;

    protected TextAnnotation(BPMNModel model, Element node, String type, BPMNProcess bpmnProcess)
            throws BPMNModelException {
        super(model, node, type, bpmnProcess);
    }

    @Override
    public double getDefaultWidth() {
        return DEFAULT_WIDTH;
    }

    @Override
    public double getDefaultHeight() {
        return DEFAULT_HEIGHT;
    }

    /**
     * Returns the Documentation
     * 
     * @return String - can be empty
     */
    public String getText() {
        return this.getChildNodeContent(BPMNNS.BPMN2, "text");

    }

    /**
     * Set the new documentation content for this element.
     * 
     * @param content
     */
    public void setText(String content) {
        this.setChildNodeContent(BPMNNS.BPMN2, "text", content, true);
    }

}
