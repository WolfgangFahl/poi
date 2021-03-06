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

package org.apache.poi.xddf.usermodel.chart;

import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.HasShapeProperties;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTAxPos;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChartLines;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTCrosses;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLogBase;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumFmt;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScaling;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTickMark;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTUnsignedInt;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

/**
 * Base class for all axis types.
 */
@Beta
public abstract class XDDFChartAxis implements HasShapeProperties {
    protected abstract CTUnsignedInt getCTAxId();

    protected abstract CTAxPos getCTAxPos();

    protected abstract CTNumFmt getCTNumFmt();

    protected abstract CTScaling getCTScaling();

    protected abstract CTCrosses getCTCrosses();

    protected abstract CTBoolean getDelete();

    protected abstract CTTickMark getMajorCTTickMark();

    protected abstract CTTickMark getMinorCTTickMark();

    public abstract XDDFShapeProperties getOrAddMajorGridProperties();

    public abstract XDDFShapeProperties getOrAddMinorGridProperties();

    /**
     * @return axis id
     */
    public long getId() {
        return getCTAxId().getVal();
    }

    /**
     * @return axis position
     */
    public AxisPosition getPosition() {
        return AxisPosition.valueOf(getCTAxPos().getVal());
    }

    /**
     * @param position
     *            new axis position
     */
    public void setPosition(AxisPosition position) {
        getCTAxPos().setVal(position.underlying);
    }

    /**
     * Use this to check before retrieving a number format, as calling
     * {@link #getNumberFormat()} may create a default one if none exists.
     *
     * @return true if a number format element is defined, false if not
     */
    public abstract boolean hasNumberFormat();

    /**
     * @param format
     *            axis number format
     */
    public void setNumberFormat(String format) {
        getCTNumFmt().setFormatCode(format);
        getCTNumFmt().setSourceLinked(true);
    }

    /**
     * @return axis number format
     */
    public String getNumberFormat() {
        return getCTNumFmt().getFormatCode();
    }

    /**
     * @return true if log base is defined, false otherwise
     */
    public boolean isSetLogBase() {
        return getCTScaling().isSetLogBase();
    }

    private static final double MIN_LOG_BASE = 2.0;
    private static final double MAX_LOG_BASE = 1000.0;

    /**
     * @param logBase
     *            a number between 2 and 1000 (inclusive)
     * @throws IllegalArgumentException
     *             if log base not within allowed range
     */
    public void setLogBase(double logBase) {
        if (logBase < MIN_LOG_BASE || MAX_LOG_BASE < logBase) {
            throw new IllegalArgumentException("Axis log base must be between 2 and 1000 (inclusive), got: " + logBase);
        }
        CTScaling scaling = getCTScaling();
        if (scaling.isSetLogBase()) {
            scaling.getLogBase().setVal(logBase);
        } else {
            scaling.addNewLogBase().setVal(logBase);
        }
    }

    /**
     * @return axis log base or 0.0 if not set
     */
    public double getLogBase() {
        CTLogBase logBase = getCTScaling().getLogBase();
        if (logBase != null) {
            return logBase.getVal();
        }
        return 0.0;
    }

    /**
     * @return true if minimum value is defined, false otherwise
     */
    public boolean isSetMinimum() {
        return getCTScaling().isSetMin();
    }

    /**
     * @param min
     *            axis minimum
     */
    public void setMinimum(double min) {
        CTScaling scaling = getCTScaling();
        if (scaling.isSetMin()) {
            scaling.getMin().setVal(min);
        } else {
            scaling.addNewMin().setVal(min);
        }
    }

    /**
     * @return axis minimum or 0.0 if not set
     */
    public double getMinimum() {
        CTScaling scaling = getCTScaling();
        if (scaling.isSetMin()) {
            return scaling.getMin().getVal();
        } else {
            return 0.0;
        }
    }

    /**
     * @return true if maximum value is defined, false otherwise
     */
    public boolean isSetMaximum() {
        return getCTScaling().isSetMax();
    }

    /**
     * @param max
     *            axis maximum
     */
    public void setMaximum(double max) {
        CTScaling scaling = getCTScaling();
        if (scaling.isSetMax()) {
            scaling.getMax().setVal(max);
        } else {
            scaling.addNewMax().setVal(max);
        }
    }

    /**
     * @return axis maximum or 0.0 if not set
     */
    public double getMaximum() {
        CTScaling scaling = getCTScaling();
        if (scaling.isSetMax()) {
            return scaling.getMax().getVal();
        } else {
            return 0.0;
        }
    }

    /**
     * @return axis orientation
     */
    public AxisOrientation getOrientation() {
        return AxisOrientation.valueOf(getCTScaling().getOrientation().getVal());
    }

    /**
     * @param orientation
     *            axis orientation
     */
    public void setOrientation(AxisOrientation orientation) {
        CTScaling scaling = getCTScaling();
        if (scaling.isSetOrientation()) {
            scaling.getOrientation().setVal(orientation.underlying);
        } else {
            scaling.addNewOrientation().setVal(orientation.underlying);
        }
    }

    /**
     * @return axis cross type
     */
    public AxisCrosses getCrosses() {
        return AxisCrosses.valueOf(getCTCrosses().getVal());
    }

    /**
     * @param crosses
     *            axis cross type
     */
    public void setCrosses(AxisCrosses crosses) {
        getCTCrosses().setVal(crosses.underlying);
    }

    /**
     * Declare this axis cross another axis.
     *
     * @param axis
     *            that this axis should cross
     */
    public abstract void crossAxis(XDDFChartAxis axis);

    /**
     * @return visibility of the axis.
     */
    public boolean isVisible() {
        return !getDelete().getVal();
    }

    /**
     * @param value
     *            visibility of the axis.
     */
    public void setVisible(boolean value) {
        getDelete().setVal(!value);
    }

    /**
     * @return major tick mark.
     */
    public AxisTickMark getMajorTickMark() {
        return AxisTickMark.valueOf(getMajorCTTickMark().getVal());
    }

    /**
     * @param tickMark
     *            major tick mark type.
     */
    public void setMajorTickMark(AxisTickMark tickMark) {
        getMajorCTTickMark().setVal(tickMark.underlying);
    }

    /**
     * @return minor tick mark.
     */
    public AxisTickMark getMinorTickMark() {
        return AxisTickMark.valueOf(getMinorCTTickMark().getVal());
    }

    /**
     * @param tickMark
     *            minor tick mark type.
     */
    public void setMinorTickMark(AxisTickMark tickMark) {
        getMinorCTTickMark().setVal(tickMark.underlying);
    }

    protected CTShapeProperties getOrAddLinesProperties(CTChartLines gridlines) {
        CTShapeProperties properties;
        if (gridlines.isSetSpPr()) {
            properties = gridlines.getSpPr();
        } else {
            properties = gridlines.addNewSpPr();
        }
        return properties;
    }

    protected long getNextAxId(CTPlotArea plotArea) {
        long totalAxisCount = plotArea.sizeOfValAxArray() + plotArea.sizeOfCatAxArray() + plotArea.sizeOfDateAxArray()
                + plotArea.sizeOfSerAxArray();
        return totalAxisCount;
    }
}
