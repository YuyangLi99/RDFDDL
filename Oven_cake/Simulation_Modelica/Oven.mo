model Oven
  parameter Modelica.Units.SI.Temperature TAmb(final displayUnit = "degC") = 293.15
    "Ambient temperature";
  parameter Modelica.Units.SI.TemperatureDifference TDif (final displayUnit = "K")= 1
    "Error in temperature";
  output Modelica.Units.SI.Temperature TRes(displayUnit="degC") = resistor.T_heatPort "Resulting temperature";
  Modelica.Electrical.Analog.Basic.Ground ground
    annotation (Placement(transformation(origin = {-8, 56}, extent = {{-100, -100}, {-80, -80}})));
  Modelica.Electrical.Analog.Sources.ConstantVoltage constantVoltage(V= 220)
    annotation (Placement(
        transformation(
        origin={-98,6},
        extent={{-10,-10},{10,10}},
        rotation=270)));
  Modelica.Thermal.HeatTransfer.Components.HeatCapacitor heatCapacitor(C= 4000, T(start=TAmb, fixed=
          true))
    annotation (Placement(transformation(origin = {-8, 56}, extent = {{0, -60}, {20, -80}})));
  Modelica.Electrical.Analog.Basic.Resistor resistor(
    R= 20,
    T_ref=293.15,
    alpha=0,
    useHeatPort=true) annotation (Placement(transformation(
        origin={-38,6},
        extent={{-10,10},{10,-10}},
        rotation=270)));
  Modelica.Thermal.HeatTransfer.Sources.FixedTemperature fixedTemperature(final T = TAmb)
    annotation (Placement(transformation(origin = {-8, 56}, extent = {{100, -60}, {80, -40}})));
  Modelica.Thermal.HeatTransfer.Celsius.TemperatureSensor temperatureSensor annotation (
      Placement(transformation(
        origin={2,26},
        extent={{-10,-10},{10,10}},
        rotation=90)));
  Modelica.Thermal.HeatTransfer.Components.ThermalConductor thermalConductor(G= 10)
    annotation (Placement(transformation(origin = {-8, 56}, extent = {{40, -60}, {60, -40}})));
  Modelica.Electrical.Analog.Ideal.IdealOpeningSwitch idealSwitch
        annotation (Placement(transformation(origin = {-8, 56}, extent = {{-70, -50}, {-50, -30}})));
  Modelica.Blocks.Logical.OnOffController onOffController(final bandwidth = TDif)
    annotation (Placement(transformation(origin = {-8, 56}, extent = {{0, -20}, {-20, 0}})));
  Modelica.Blocks.Logical.Not logicalNot annotation(
    Placement(transformation(origin = {-8, 56}, extent = {{-30, -20}, {-50, 0}})));
  Modelica.Blocks.Sources.Constant targetTemperature(k = 180)  annotation(
    Placement(transformation(origin = {32, 52}, extent = {{10, -10}, {-10, 10}})));
equation
  connect(constantVoltage.n, resistor.n) annotation(
    Line(points = {{-98, -4}, {-38, -4}}, color = {0, 0, 255}));
  connect(constantVoltage.n, ground.p) annotation(
    Line(points = {{-98, -4}, {-98, -24}}, color = {0, 0, 255}));
  connect(resistor.heatPort, thermalConductor.port_a) annotation(
    Line(points = {{-28, 6}, {32, 6}}, color = {191, 0, 0}));
  connect(thermalConductor.port_b, fixedTemperature.port) annotation(
    Line(points = {{52, 6}, {72, 6}}, color = {191, 0, 0}));
  connect(resistor.heatPort, temperatureSensor.port) annotation(
    Line(points = {{-28, 6}, {2, 6}, {2, 16}}, color = {191, 0, 0}));
  connect(resistor.heatPort, heatCapacitor.port) annotation(
    Line(points = {{-28, 6}, {2, 6}, {2, -4}}, color = {191, 0, 0}));
  connect(constantVoltage.p, idealSwitch.p) annotation(
    Line(points = {{-98, 16}, {-78, 16}}, color = {0, 0, 255}));
  connect(idealSwitch.n, resistor.p) annotation(
    Line(points = {{-58, 16}, {-38, 16}}, color = {0, 0, 255}));
  connect(temperatureSensor.T, onOffController.u) annotation(
    Line(points = {{2, 36}, {2, 40}, {-6, 40}}, color = {0, 0, 127}));
  connect(onOffController.y, logicalNot.u) annotation(
    Line(points = {{-29, 46}, {-36, 46}}, color = {255, 0, 255}));
  connect(logicalNot.y, idealSwitch.control) annotation(
    Line(points = {{-59, 46}, {-68, 46}, {-68, 28}}, color = {255, 0, 255}));
  connect(onOffController.reference, targetTemperature.y) annotation(
    Line(points = {{-6, 52}, {21, 52}}, color = {0, 0, 127}));
  annotation(
    uses(Modelica(version = "4.0.0")),
  Diagram(coordinateSystem(extent = {{-300, -300}, {300, 300}})),
  Icon(coordinateSystem(extent = {{-300, -300}, {300, 300}})),
  version = "");
end Oven;
