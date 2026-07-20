/* 
 * Copyright (c) 2014, Andrea Vacondio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package uet.fit.client.ui.view;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.property.DoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.SizeConverter;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Progress indicator showing a filling arc.
 * 
 * @author Andrea Vacondio
 *
 */
public class RingProgressIndicator extends ProgressCircleIndicator {

    public RingProgressIndicator() {
        this.getStylesheets().add(getClass().getResource("/css/ringprogress.css").toExternalForm());
        this.getStyleClass().add("ringindicator");
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new RingProgressIndicatorSkin(this);
    }

    public final void setRingWidth(int value) {
        ringWidthProperty().set(value);
    }

    public final DoubleProperty ringWidthProperty() {
        return ringWidth;
    }

    public final double getRingWidth() {
        return ringWidthProperty().get();
    }

    /**
     * thickness of the ring indicator.
     */
    private final DoubleProperty ringWidth = new StyleableDoubleProperty(22) {
        @Override
        public Object getBean() {
            return RingProgressIndicator.this;
        }

        @Override
        public String getName() {
            return "ringWidth";
        }

        @Override
        public CssMetaData<RingProgressIndicator, Number> getCssMetaData() {
            return StyleableProperties.RING_WIDTH;
        }
    };

    private static class StyleableProperties {
        private static final CssMetaData<RingProgressIndicator, Number> RING_WIDTH = new CssMetaData<RingProgressIndicator, Number>(
                "-fx-ring-width", SizeConverter.getInstance(), 22) {

            @Override
            public boolean isSettable(RingProgressIndicator n) {
                return !n.ringWidth.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(RingProgressIndicator n) {
                return (StyleableProperty<Number>) n.ringWidth;
            }
        };

        public static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Control.getClassCssMetaData());
            styleables.addAll(ProgressCircleIndicator.getClassCssMetaData());
            styleables.add(RING_WIDTH);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * Skin of the ring progress indicator where an arc grows and by the progress value up to 100% where the arc becomes a ring.
     *
     * @author Andrea Vacondio
     *
     */
    public static class RingProgressIndicatorSkin implements Skin<RingProgressIndicator> {

        private final RingProgressIndicator indicator;
        private final Label percentLabel = new Label();
        private final Circle innerCircle = new Circle();
        private final Circle outerCircle = new Circle();
        private final StackPane container = new StackPane();
        private final Arc fillerArc = new Arc();
        private final Transition transition = new Animation();

        public RingProgressIndicatorSkin(final RingProgressIndicator indicator) {
            this.indicator = indicator;
            initContainer(indicator);
            initFillerArc();
            container.widthProperty().addListener((o, oldVal, newVal) -> fillerArc.setCenterX(newVal.doubleValue() / 2));
            container.heightProperty().addListener((o, oldVal, newVal) -> fillerArc.setCenterY(newVal.doubleValue() / 2));
            innerCircle.getStyleClass().add("ringindicator-inner-circle");
            outerCircle.getStyleClass().add("ringindicator-outer-circle-secondary");
            updateRadii();

            this.indicator.indeterminateProperty().addListener((o, oldVal, newVal) -> initIndeterminate(newVal));
            this.indicator.progressProperty().addListener((o, oldVal, newVal) -> {
                if (newVal.intValue() >= 0) {
                    setProgressLabel(newVal.intValue());
                    fillerArc.setLength(newVal.intValue() * -3.6);
                }
            });
            this.indicator.ringWidthProperty().addListener((o, oldVal, newVal) -> updateRadii());
            innerCircle.strokeWidthProperty().addListener(e -> updateRadii());
            innerCircle.radiusProperty().addListener(e -> updateRadii());
            initTransition();
            initIndeterminate(indicator.isIndeterminate());
            initLabel(indicator.getProgress());
            indicator.visibleProperty().addListener((o, oldVal, newVal) -> {
                if (newVal && this.indicator.isIndeterminate()) {
                    transition.play();
                } else {
                    transition.pause();
                }
            });
            container.getChildren().addAll(fillerArc, outerCircle, innerCircle, percentLabel);
        }

        private void setProgressLabel(int value) {
            if (value >= 0) {
                percentLabel.setText(String.format("%d%%", value));
            }
        }

        private void initTransition() {
            fillerArc.setType(ArcType.OPEN);
            transition.setAutoReverse(false);
            transition.setCycleCount(Animation.INDEFINITE);
            transition.setDelay(Duration.ZERO);
            transition.setInterpolator(Interpolator.EASE_BOTH);
        }

        private void initFillerArc() {
            fillerArc.setManaged(false);
            fillerArc.getStyleClass().add("ringindicator-filler");
            fillerArc.setStartAngle(90);
            fillerArc.setLength(indicator.getProgress() * -3.6);
        }

        private void initContainer(final RingProgressIndicator indicator) {
            container.getStylesheets().addAll(indicator.getStylesheets());
            container.getStyleClass().addAll("circleindicator-container");
            container.setMaxHeight(Region.USE_PREF_SIZE);
            container.setMaxWidth(Region.USE_PREF_SIZE);
        }

        private void updateRadii() {
            double ringWidth = indicator.getRingWidth();
            double innerCircleHalfStrokeWidth = innerCircle.getStrokeWidth() / 2;
            double innerCircleRadius = indicator.getInnerCircleRadius();
            outerCircle.setRadius(innerCircleRadius + innerCircleHalfStrokeWidth + ringWidth);
            fillerArc.setRadiusY(innerCircleRadius + innerCircleHalfStrokeWidth - 1 + (ringWidth / 2));
            fillerArc.setRadiusX(innerCircleRadius + innerCircleHalfStrokeWidth - 1 + (ringWidth / 2));
            fillerArc.setStrokeWidth(ringWidth);
            innerCircle.setRadius(innerCircleRadius);
        }

        private void initLabel(int value) {
            setProgressLabel(value);
            percentLabel.getStyleClass().add("circleindicator-label");
        }

        private void initIndeterminate(boolean newVal) {
            percentLabel.setVisible(!newVal);
            if (newVal) {
                fillerArc.setLength(360);
                fillerArc.getStyleClass().add("indeterminate");
                if (indicator.isVisible()) {
                    transition.play();
                }
            } else {
                fillerArc.getStyleClass().remove("indeterminate");
                fillerArc.setRotate(0);
                transition.stop();
            }
        }

        @Override
        public RingProgressIndicator getSkinnable() {
            return indicator;
        }

        @Override
        public Node getNode() {
            return container;
        }

        @Override
        public void dispose() {
            transition.stop();
        }

        private class Animation extends Transition {

            public Animation() {
                setCycleDuration(Duration.millis(1000));
            }

            @Override
            protected void interpolate(double v) {
                double len = v * 360;
                fillerArc.setLength(len);
                fillerArc.setStartAngle(v * 360 + 90);
            }
        }
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
    	 return StyleableProperties.STYLEABLES;
    }
}
