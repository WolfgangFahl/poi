/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.xssf.usermodel;

import static org.apache.poi.POIXMLTypeLoader.DEFAULT_XML_OPTIONS;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.poi.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.Internal;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.model.CommentsTable;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTConnector;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTDrawing;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTGraphicalObjectFrame;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTGroupShape;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTMarker;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTOneCellAnchor;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTPicture;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTShape;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTTwoCellAnchor;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.STEditAs;

/**
 * Represents a SpreadsheetML drawing
 *
 * @author Yegor Kozlov
 */
public final class XSSFDrawing extends POIXMLDocumentPart implements Drawing {
    /**
     * Root element of the SpreadsheetML Drawing part
     */
    private CTDrawing drawing;
    private long numOfGraphicFrames = 0L;
    
    protected static final String NAMESPACE_A = "http://schemas.openxmlformats.org/drawingml/2006/main";
    protected static final String NAMESPACE_C = "http://schemas.openxmlformats.org/drawingml/2006/chart";

    /**
     * Create a new SpreadsheetML drawing
     *
     * @see org.apache.poi.xssf.usermodel.XSSFSheet#createDrawingPatriarch()
     */
    protected XSSFDrawing() {
        super();
        drawing = newDrawing();
    }

    /**
     * Construct a SpreadsheetML drawing from a package part
     *
     * @param part the package part holding the drawing data,
     * the content type must be <code>application/vnd.openxmlformats-officedocument.drawing+xml</code>
     * @param rel  the package relationship holding this drawing,
     * the relationship type must be http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing
     */
    public XSSFDrawing(PackagePart part, PackageRelationship rel) throws IOException, XmlException {
        super(part, rel);
        XmlOptions options  = new XmlOptions(DEFAULT_XML_OPTIONS);
        //Removing root element
        options.setLoadReplaceDocumentElement(null);
        drawing = CTDrawing.Factory.parse(part.getInputStream(),options);
    }

    /**
     * Construct a new CTDrawing bean. By default, it's just an empty placeholder for drawing objects
     *
     * @return a new CTDrawing bean
     */
    private static CTDrawing newDrawing(){
        return CTDrawing.Factory.newInstance();
    }

    /**
     * Return the underlying CTDrawing bean, the root element of the SpreadsheetML Drawing part.
     *
     * @return the underlying CTDrawing bean
     */
    @Internal
    public CTDrawing getCTDrawing(){
        return drawing;
    }

    @Override
    protected void commit() throws IOException {
        XmlOptions xmlOptions = new XmlOptions(DEFAULT_XML_OPTIONS);

        /*
            Saved drawings must have the following namespaces set:
            <xdr:wsDr
                xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                xmlns:xdr="http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing">
        */
        xmlOptions.setSaveSyntheticDocumentElement(
                new QName(CTDrawing.type.getName().getNamespaceURI(), "wsDr", "xdr")
        );

        PackagePart part = getPackagePart();
        OutputStream out = part.getOutputStream();
        drawing.save(out, xmlOptions);
        out.close();
    }

	@Override
    public XSSFClientAnchor createAnchor(int dx1, int dy1, int dx2, int dy2,
			int col1, int row1, int col2, int row2) {
		return new XSSFClientAnchor(dx1, dy1, dx2, dy2, col1, row1, col2, row2);
	}

    /**
     * Constructs a textbox under the drawing.
     *
     * @param anchor    the client anchor describes how this group is attached
     *                  to the sheet.
     * @return      the newly created textbox.
     */
    public XSSFTextBox createTextbox(XSSFClientAnchor anchor){
        long shapeId = newShapeId();
        CTTwoCellAnchor ctAnchor = createTwoCellAnchor(anchor);
        CTShape ctShape = ctAnchor.addNewSp();
        ctShape.set(XSSFSimpleShape.prototype());
        ctShape.getNvSpPr().getCNvPr().setId(shapeId);
        XSSFTextBox shape = new XSSFTextBox(this, ctShape);
        shape.anchor = anchor;
        return shape;

    }

    /**
     * Creates a picture.
     *
     * @param anchor    the client anchor describes how this picture is attached to the sheet.
     * @param pictureIndex the index of the picture in the workbook collection of pictures,
     *   {@link org.apache.poi.xssf.usermodel.XSSFWorkbook#getAllPictures()} .
     *
     * @return  the newly created picture shape.
     */
    public XSSFPicture createPicture(XSSFClientAnchor anchor, int pictureIndex)
    {
        PackageRelationship rel = addPictureReference(pictureIndex);

        long shapeId = newShapeId();
        CTTwoCellAnchor ctAnchor = createTwoCellAnchor(anchor);
        CTPicture ctShape = ctAnchor.addNewPic();
        ctShape.set(XSSFPicture.prototype());

        ctShape.getNvPicPr().getCNvPr().setId(shapeId);

        XSSFPicture shape = new XSSFPicture(this, ctShape);
        shape.anchor = anchor;
        shape.setPictureReference(rel);
        return shape;
    }

    @Override
    public XSSFPicture createPicture(ClientAnchor anchor, int pictureIndex){
        return createPicture((XSSFClientAnchor)anchor, pictureIndex);
    }

	/**
	 * Creates a chart.
	 * @param anchor the client anchor describes how this chart is attached to
	 *               the sheet.
	 * @return the newly created chart
	 * @see org.apache.poi.xssf.usermodel.XSSFDrawing#createChart(ClientAnchor)
	 */
    public XSSFChart createChart(XSSFClientAnchor anchor) {
        int chartNumber = getPackagePart().getPackage().
            getPartsByContentType(XSSFRelation.CHART.getContentType()).size() + 1;

        XSSFChart chart = (XSSFChart) createRelationship(
                XSSFRelation.CHART, XSSFFactory.getInstance(), chartNumber);
        String chartRelId = chart.getPackageRelationship().getId();

        XSSFGraphicFrame frame = createGraphicFrame(anchor);
        frame.setChart(chart, chartRelId);

        return chart;
    }

	@Override
    public XSSFChart createChart(ClientAnchor anchor) {
		return createChart((XSSFClientAnchor)anchor);
	}

    /**
     * Add the indexed picture to this drawing relations
     *
     * @param pictureIndex the index of the picture in the workbook collection of pictures,
     *   {@link org.apache.poi.xssf.usermodel.XSSFWorkbook#getAllPictures()} .
     */
    protected PackageRelationship addPictureReference(int pictureIndex){
        XSSFWorkbook wb = (XSSFWorkbook)getParent().getParent();
        XSSFPictureData data = wb.getAllPictures().get(pictureIndex);
        PackagePartName ppName = data.getPackagePart().getPartName();
        PackageRelationship rel = getPackagePart().addRelationship(ppName, TargetMode.INTERNAL, XSSFRelation.IMAGES.getRelation());
        addRelation(rel.getId(),new XSSFPictureData(data.getPackagePart(), rel));
        return rel;
    }

    /**
     * Creates a simple shape.  This includes such shapes as lines, rectangles,
     * and ovals.
     *
     * @param anchor    the client anchor describes how this group is attached
     *                  to the sheet.
     * @return  the newly created shape.
     */
    public XSSFSimpleShape createSimpleShape(XSSFClientAnchor anchor)
    {
        long shapeId = newShapeId();
        CTTwoCellAnchor ctAnchor = createTwoCellAnchor(anchor);
        CTShape ctShape = ctAnchor.addNewSp();
        ctShape.set(XSSFSimpleShape.prototype());
        ctShape.getNvSpPr().getCNvPr().setId(shapeId);
        XSSFSimpleShape shape = new XSSFSimpleShape(this, ctShape);
        shape.anchor = anchor;
        return shape;
    }

    /**
     * Creates a simple shape.  This includes such shapes as lines, rectangles,
     * and ovals.
     *
     * @param anchor    the client anchor describes how this group is attached
     *                  to the sheet.
     * @return  the newly created shape.
     */
    public XSSFConnector createConnector(XSSFClientAnchor anchor)
    {
        CTTwoCellAnchor ctAnchor = createTwoCellAnchor(anchor);
        CTConnector ctShape = ctAnchor.addNewCxnSp();
        ctShape.set(XSSFConnector.prototype());

        XSSFConnector shape = new XSSFConnector(this, ctShape);
        shape.anchor = anchor;
        return shape;
    }

    /**
     * Creates a simple shape.  This includes such shapes as lines, rectangles,
     * and ovals.
     *
     * @param anchor    the client anchor describes how this group is attached
     *                  to the sheet.
     * @return  the newly created shape.
     */
    public XSSFShapeGroup createGroup(XSSFClientAnchor anchor)
    {
        CTTwoCellAnchor ctAnchor = createTwoCellAnchor(anchor);
        CTGroupShape ctGroup = ctAnchor.addNewGrpSp();
        ctGroup.set(XSSFShapeGroup.prototype());

        XSSFShapeGroup shape = new XSSFShapeGroup(this, ctGroup);
        shape.anchor = anchor;
        return shape;
    }

	/**
	 * Creates a comment.
	 * @param anchor the client anchor describes how this comment is attached
	 *               to the sheet.
	 * @return the newly created comment.
	 */
    @Override
    public XSSFComment createCellComment(ClientAnchor anchor) {
        XSSFClientAnchor ca = (XSSFClientAnchor)anchor;
        XSSFSheet sheet = (XSSFSheet)getParent();

        //create comments and vmlDrawing parts if they don't exist
        CommentsTable comments = sheet.getCommentsTable(true);
        XSSFVMLDrawing vml = sheet.getVMLDrawing(true);
        com.microsoft.schemas.vml.CTShape vmlShape = vml.newCommentShape();
        if(ca.isSet()){
            // convert offsets from emus to pixels since we get a DrawingML-anchor
            // but create a VML Drawing
            int dx1Pixels = ca.getDx1()/Units.EMU_PER_PIXEL;
            int dy1Pixels = ca.getDy1()/Units.EMU_PER_PIXEL;
            int dx2Pixels = ca.getDx2()/Units.EMU_PER_PIXEL;
            int dy2Pixels = ca.getDy2()/Units.EMU_PER_PIXEL;
            String position =
                    ca.getCol1() + ", " + dx1Pixels + ", " +
                    ca.getRow1() + ", " + dy1Pixels + ", " +
                    ca.getCol2() + ", " + dx2Pixels + ", " +
                    ca.getRow2() + ", " + dy2Pixels;
            vmlShape.getClientDataArray(0).setAnchorArray(0, position);
        }
        String ref = new CellReference(ca.getRow1(), ca.getCol1()).formatAsString();

        if(comments.findCellComment(ref) != null) {
            throw new IllegalArgumentException("Multiple cell comments in one cell are not allowed, cell: " + ref);
        }
        
        return new XSSFComment(comments, comments.newComment(ref), vmlShape);
    }

    /**
     * Creates a new graphic frame.
     *
     * @param anchor    the client anchor describes how this frame is attached
     *                  to the sheet
     * @return  the newly created graphic frame
     */
    private XSSFGraphicFrame createGraphicFrame(XSSFClientAnchor anchor) {
        CTTwoCellAnchor ctAnchor = createTwoCellAnchor(anchor);
        CTGraphicalObjectFrame ctGraphicFrame = ctAnchor.addNewGraphicFrame();
        ctGraphicFrame.set(XSSFGraphicFrame.prototype());

        long frameId = numOfGraphicFrames++;
        XSSFGraphicFrame graphicFrame = new XSSFGraphicFrame(this, ctGraphicFrame);
        graphicFrame.setAnchor(anchor);
        graphicFrame.setId(frameId);
        graphicFrame.setName("Diagramm" + frameId);
        return graphicFrame;
    }
    
    /**
     * Returns all charts in this drawing.
     */
    public List<XSSFChart> getCharts() {
       List<XSSFChart> charts = new ArrayList<XSSFChart>();
       for(POIXMLDocumentPart part : getRelations()) {
          if(part instanceof XSSFChart) {
             charts.add((XSSFChart)part);
          }
       }
       return charts;
    }

    /**
     * Create and initialize a CTTwoCellAnchor that anchors a shape against top-left and bottom-right cells.
     *
     * @return a new CTTwoCellAnchor
     */
    private CTTwoCellAnchor createTwoCellAnchor(XSSFClientAnchor anchor) {
        CTTwoCellAnchor ctAnchor = drawing.addNewTwoCellAnchor();
        ctAnchor.setFrom(anchor.getFrom());
        ctAnchor.setTo(anchor.getTo());
        ctAnchor.addNewClientData();
        anchor.setTo(ctAnchor.getTo());
        anchor.setFrom(ctAnchor.getFrom());
        STEditAs.Enum aditAs;
        switch(anchor.getAnchorType()) {
            case ClientAnchor.DONT_MOVE_AND_RESIZE: aditAs = STEditAs.ABSOLUTE; break;
            case ClientAnchor.MOVE_AND_RESIZE: aditAs = STEditAs.TWO_CELL; break;
            case ClientAnchor.MOVE_DONT_RESIZE: aditAs = STEditAs.ONE_CELL; break;
            default: aditAs = STEditAs.ONE_CELL;
        }
        ctAnchor.setEditAs(aditAs);
        return ctAnchor;
    }

    private long newShapeId(){
        return drawing.sizeOfTwoCellAnchorArray() + 1;
    }

    /**
     *
     * @return list of shapes in this drawing
     */
    public List<XSSFShape>  getShapes(){
        List<XSSFShape> lst = new ArrayList<XSSFShape>();
        for(XmlObject obj : drawing.selectPath("./*/*")) {
            XSSFShape shape = null;
            if(obj instanceof CTPicture) shape = new XSSFPicture(this, (CTPicture)obj) ;
            else if(obj instanceof CTConnector) shape = new XSSFConnector(this, (CTConnector)obj) ;
            else if(obj instanceof CTShape) shape = new XSSFSimpleShape(this, (CTShape)obj) ;
            else if(obj instanceof CTGraphicalObjectFrame) shape = new XSSFGraphicFrame(this, (CTGraphicalObjectFrame)obj) ;
            else if(obj instanceof CTGroupShape) shape = new XSSFShapeGroup(this, (CTGroupShape)obj) ;

            if(shape != null){
                shape.anchor = getAnchorFromParent(obj);
                lst.add(shape);
            }
        }
        return lst;
    }


    private XSSFAnchor getAnchorFromParent(XmlObject obj){
        XSSFAnchor anchor = null;

        XmlObject parentXbean = null;
        XmlCursor cursor = obj.newCursor();
        if(cursor.toParent()) parentXbean = cursor.getObject();
        cursor.dispose();
        if(parentXbean != null){
            if (parentXbean instanceof CTTwoCellAnchor) {
                CTTwoCellAnchor ct = (CTTwoCellAnchor)parentXbean;
                anchor = new XSSFClientAnchor(ct.getFrom(), ct.getTo());
            } else if (parentXbean instanceof CTOneCellAnchor) {
                CTOneCellAnchor ct = (CTOneCellAnchor)parentXbean;
                anchor = new XSSFClientAnchor(ct.getFrom(), CTMarker.Factory.newInstance());
            }
        }
        return anchor;
    }

}
