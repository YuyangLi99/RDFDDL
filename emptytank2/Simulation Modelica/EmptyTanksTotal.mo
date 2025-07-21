package n0
  extends n1.n2.n3;

  package n4
    extends n1.n2.n3;
    final constant Real n5 = 1e-15;
    final constant Real n6 = 1e-60;
    final constant Real n7 = 1e60;
    final constant Integer n8 = OpenModelica.Internal.Architecture.integerMax();
  end n4;
  annotation(version = "4.0.0", versionDate = "2020-06-04", dateModified = "2020-06-04 11:00:00Z");
end n0;

package n1
  extends n1.n2.n3;

  package n9
    extends n1.n2.n3;
    import n1.n10.n11;

    package n12
      extends n1.n2.n13;
      connector n14 = input Real;
      connector n15 = input Boolean;
    end n12;
  end n9;

  package n16
    extends n1.n2.n3;
    import n1.n10.n11;
    import n17 = n1.n10.n18;

    package n19
      extends n1.n2.n20;

      package n21
        extends n1.n2.n20;

        model n22
          extends n1.n2.n23;
          n1.n16.n25.n26 n24(redeclare package n27 = n1.n28.n29.n30, n31 = 1, n32 = 1, n33 = 1, n34 = {n1.n16.n25.n36.n37(n35 = 0.1)}, n38 = 1.1);
          n1.n16.n40.n41 n39(redeclare package n27 = n1.n28.n29.n30, n42 = 1, n35 = 0.1, n43 = -1);
          n1.n16.n25.n26 n44(n32 = 1, redeclare package n27 = n1.n28.n29.n30, n31 = 1, n38 = 1.1, n34 = {n1.n16.n25.n36.n37(n35 = 0.1, n38 = 0.5)}, n33 = 1.0e-10);
          inner n1.n16.n46 n45(n47 = n1.n16.n48.n49.n50);
        equation
          connect(n24.n51[1], n39.n52);
          connect(n39.n53, n44.n51[1]);
          annotation(experiment(StopTime = 50));
        end n22;
      end n21;
    end n19;

    model n46
      parameter n11.n55 n54 = 101325;
      parameter n11.n57 n56 = 293.15;
      parameter n11.n59 n58 = n1.n60.n61;
      parameter Boolean n62 = true annotation(Evaluate = true);
      parameter n1.n16.n48.n49 n47 = n1.n16.n48.n49.n63 annotation(Evaluate = true);
      parameter n1.n16.n48.n49 n64 = n47 annotation(Evaluate = true);
      final parameter n1.n16.n48.n49 n65 = n64 annotation(Evaluate = true);
      final parameter n1.n16.n48.n49 n66 = n64 annotation(Evaluate = true);
      parameter n1.n16.n48.n49 n67 = n1.n16.n48.n49.n68 annotation(Evaluate = true);
      parameter n11.n70 n69 = 0;
      parameter n11.n55 n71 = n54;
      parameter n11.n57 n72 = n56;
      parameter Boolean n73 = false annotation(Evaluate = true);
      parameter n11.n70 n74 = if n73 then 1 else 1e2*n75;
      parameter Real n76(min = 0) = 1e-4;
      parameter n11.n55 n77(min = 0) = 1;
      parameter n11.n70 n75(min = 0) = 1e-2;
    end n46;

    package n25
      extends n1.n2.n78;

      model n26
        import n1.n60.n79;
        n11.n81 n80(stateSelect = StateSelect.prefer, start = n82);
        n11.n84 n83(stateSelect = StateSelect.never);
        parameter n11.n81 n38;
        parameter n11.n85 n32;
        parameter n27.n55 n54 = n45.n54;
        parameter n27.n57 n56 = n45.n56;
        parameter n11.n81 n33(min = 0) = 0.5*n38;
        extends n1.n16.n25.n36.n86(final n87 = n83, final n88 = n80, final n89 = n38, final n90 = n32, n91(n92 = {n32 + 2*sqrt(n32*n79)*n80}), final n93 = false, final n71 = n54);
      protected
        final parameter n11.n81 n82 = max(n33, n1.n60.n5);
      initial equation
        if n64 == n48.n49.n50 then
          n80 = n82;
        elseif n64 == n48.n49.n102 then
          der(n80) = 0;
        end if;
      equation
        n83 = n32*n80;
        n94.n95 = n54;
        if n27.n96 or n47 == n48.n49.n68 then
          n97 = 0;
        else
          n97 = -n54*der(n83);
        end if;
        for n98 in 1:n31 loop
          n99[n98] = max(0, n80 - n100[n98])*n45.n58*n94.n101 + n54;
        end for;
      end n26;

      package n36
        extends n1.n2.n103;

        partial model n86
          extends n1.n16.n12.n104;
          parameter Integer n31 = 0 annotation(Evaluate = true);
          n105 n51[n31](redeclare each package n27 = n27);
          parameter Boolean n106 = true annotation(Evaluate = true);
          parameter n1.n16.n25.n36.n37 n34[if n106 then n31 else 0];
          parameter n27.n70 n74 = if n45.n73 then n45.n74 else 1e2*n45.n75;
          parameter n11.n70 n75(min = 0) = if n45.n73 then n45.n76*n74 else n45.n75;
          parameter Boolean n107 = n45.n73 annotation(Evaluate = true);
          n27.n109 n108[n31];
          n27.n70 n110[n31, n27.n111];
          n27.n70 n112[n27.n111];
          n27.n115 n113[n31, n27.n114];
          n27.n115 n116[n27.n114];
          parameter Boolean n117 = false;
          replaceable model n118 = n1.n16.n25.n36.n118.n120 constrainedby n1.n16.n25.n36.n118.n119;
          n118 n91(redeclare package n27 = n27, final n121 = 1, final n122 = {n94.n123}, final n124 = n117);
          n1.n126.n118.n12.n127 n125 if n117;
          n27.n129 n128[n31];
          n11.n131 n130[n31];
          n11.n133 n132[n31];
          Real n134[n31](each start = n89);
          Real n135[n31];
          n11.n85 n136[n31] = {n1.n60.n79/4*n137[n98]^2 for n98 in 1:n31};
          n27.n55 n99[n31];
          constant n11.n139 n138 = 100;
          n11.n70 n140[n31];
        protected
          input n11.n81 n88 = 0;
          parameter n11.n81 n89 = 1;
          parameter n11.n85 n90 = n1.n60.n7;
          n1.n9.n12.n14 n141[n31] = n34.n35 if n106 and n31 > 0;
          n1.n9.n12.n14 n142[n31] = n34.n38 if n106 and n31 > 0;
          n1.n9.n12.n14 n143[n31] = n34.n144 if n106 and n31 > 0;
          n1.n9.n12.n14 n145[n31] = n34.n146 if n106 and n31 > 0;
          n1.n9.n12.n14 n137[n31];
          n1.n9.n12.n14 n100[n31];
          n1.n9.n12.n14 n147[n31];
          n1.n9.n12.n14 n148[n31];
          n1.n9.n12.n15 n149[n31](each start = true);
          n1.n9.n12.n15 n150[n31](each start = false);
        equation
          n151 = sum(n51.n152);
          n153 = n112;
          n154 = n116;
          n155 = sum(n108) + sum(n132);
          n156 = n91.n157[1];
          for n98 in 1:n31 loop
            assert(cardinality(n51[n98]) <= 1, "assert message 820927872");
          end for;
          assert(n88 <= n89, "assert message 1755748192");
          assert(n88 > -1e-6*n89, "assert message 1711865433");
          connect(n137, n141);
          connect(n100, n142);
          connect(n147, n143);
          connect(n148, n145);
          if not n106 then
            n137 = zeros(n31);
            n100 = zeros(n31);
            n147 = zeros(n31);
            n148 = zeros(n31);
          end if;
          for n98 in 1:n31 loop
            n128[n98] = n27.n161(n27.n160(n99[n98], inStream(n51[n98].n158), inStream(n51[n98].n159)));
            if n106 then
              n130[n98] = smooth(0, n51[n98].n152/n136[n98]/n27.n161(n27.n160(n99[n98], actualStream(n51[n98].n158), actualStream(n51[n98].n159))));
              n135[n98] = n162.n163(n88 - n100[n98] - 0.1*n137[n98], 1, 1e-3, 0.1*n137[n98]);
              n140[n98] = if not n107 then n75 else max(n75, (n1.n60.n79/8)*n137[n98]*(n27.n164(n27.n160(n99[n98], inStream(n51[n98].n158), inStream(n51[n98].n159))) + n27.n164(n94.n123))*n138);
            else
              n130[n98] = 0;
              n135[n98] = 1;
              n140[n98] = n1.n60.n7;
            end if;
            n149[n98] = n88 >= n100[n98];
            n150[n98] = not n149[n98] and (n134[n98] > 0 or n100[n98] >= n89);
            if n149[n98] then
              if n106 then
                n51[n98].n95 = n99[n98] + (0.5/n136[n98]^2*n162.n165(n51[n98].n152, n140[n98], (n147[n98] - 1 + n136[n98]^2/n90^2)/n128[n98]*n135[n98], (n148[n98] + 1 - n136[n98]^2/n90^2)/n94.n101/n135[n98]));
              else
                n51[n98].n95 = n99[n98];
              end if;
              n134[n98] = n88 - n100[n98];
            elseif n150[n98] then
              n51[n98].n95 = n99[n98];
              n134[n98] = n51[n98].n152;
            else
              n51[n98].n152 = 0;
              n134[n98] = (n51[n98].n95 - n99[n98])/n27.n166*(n100[n98] - n88);
            end if;
            n51[n98].n158 = n94.n167;
            n51[n98].n159 = n94.n168;
            n51[n98].n169 = n170;
            n108[n98] = n51[n98].n152*actualStream(n51[n98].n158);
            n132[n98] = n51[n98].n152*(0.5*n130[n98]*n130[n98] + n45.n58*n100[n98]);
            n110[n98, :] = n51[n98].n152*actualStream(n51[n98].n159);
            n113[n98, :] = n51[n98].n152*actualStream(n51[n98].n169);
          end for;
          for n98 in 1:n27.n111 loop
            n112[n98] = sum(n110[:, n98]);
          end for;
          for n98 in 1:n27.n114 loop
            n116[n98] = sum(n113[:, n98]);
          end for;
          connect(n125, n91.n171[1]);
        end n86;

        package n118
          extends n1.n2.n3;

          partial model n119
            extends n1.n16.n12.n172;
          end n119;

          model n120
            extends n119;
          equation
            n173 = n171.n174;
          end n120;
        end n118;

        record n37
          extends n1.n2.n175;
          parameter n11.n176 n35;
          parameter n11.n81 n38 = 0;
          parameter Real n146(min = 0) = 0.5;
          parameter Real n144(min = 0) = 1.04;
        end n37;

        connector n105
          extends n12.n177;
        end n105;
      end n36;
    end n25;

    package n40
      extends n1.n2.n78;

      model n41
        extends n1.n16.n40.n36.n178;
        parameter n27.n55 n179 = n45.n71;
        parameter n27.n55 n180 = n179;
        parameter n27.n70 n69 = n45.n69 annotation(Evaluate = true);
        n182 n181(redeclare package n27 = n27, final n121 = 2, n122 = {n27.n160(n52.n95, inStream(n52.n158), inStream(n52.n159)), n27.n160(n53.n95, inStream(n53.n158), inStream(n53.n159))}, n183 = {n52.n152/n27.n161(n181.n122[1])/n181.n184[1], -n53.n152/n27.n161(n181.n122[2])/n181.n184[2]}/n185, final n67 = n48.n49.n68, final n62 = n62, final n179 = n179, final n180 = n180, final n69 = n69, final n185 = n185, final n186 = {n42}, final n184 = {n32, n32}, final n187 = {4*n32/n188, 4*n32/n188}, final n189 = {n190, n190}, final n191 = {n43}, final n58 = n45.n58);
      equation
        n52.n152 = n181.n192[1];
        0 = n52.n152 + n53.n152;
        n52.n159 = inStream(n53.n159);
        n53.n159 = inStream(n52.n159);
        n52.n169 = inStream(n53.n169);
        n53.n169 = inStream(n52.n169);
        n53.n158 = inStream(n52.n158) - n45.n58*n43;
        n52.n158 = inStream(n53.n158) + n45.n58*n43;
      end n41;

      package n36
        extends n1.n2.n103;

        partial model n178
          extends n1.n16.n12.n193;
          parameter Real n185(min = 1) = 1;
          parameter n11.n194 n42;
          parameter Boolean n195 = true annotation(Evaluate = true);
          parameter n11.n176 n35;
          parameter n11.n85 n32 = n1.n60.n79*n35*n35/4;
          parameter n11.n194 n188(min = 0) = n1.n60.n79*n35;
          parameter n1.n16.n48.n196 n190 = 2.5e-5;
          final parameter n11.n84 n83 = n32*n42*n185;
          parameter n11.n194 n43 = 0;
          replaceable model n182 = n1.n16.n40.n36.n197.n199 constrainedby n1.n16.n40.n36.n197.n198;
        equation
          assert(n42 >= n43, "assert message 1069664444");
        end n178;

        package n197
          extends n1.n2.n3;

          partial model n198
            replaceable package n27 = n1.n28.n12.n200;
            parameter Integer n121 = 2;
            input n27.n201 n122[n121];
            input n11.n131 n183[n121];
            parameter Real n185;
            input n11.n85 n184[n121];
            input n11.n194 n187[n121];
            input n1.n16.n48.n196 n189[n121];
            input n11.n194 n191[n121 - 1];
            parameter n11.n59 n58 = n45.n58;
            parameter Boolean n62 = n45.n62 annotation(Evaluate = true);
            parameter n1.n16.n48.n49 n67 = n45.n67 annotation(Evaluate = true);
            parameter n27.n70 n69 = n45.n69;
            parameter n27.n55 n179;
            parameter n27.n55 n180;
            extends n1.n16.n12.n202(final n203 = n121 - 1);
            parameter Boolean n204 = true annotation(Evaluate = true);
            parameter Boolean n205 = n67 <> n48.n49.n68 annotation(Evaluate = true);
            n27.n129 n206[n121] = if n207 then fill(n208, n121) else n27.n161(n122);
            n27.n129 n209[n121 - 1];
            n27.n211 n210[n121] = if n212 then fill(n213, n121) else n27.n164(n122);
            n27.n211 n214[n121 - 1];
            n11.n216 n215[n121 - 1](each start = (n179 - n180)/(n121 - 1));
            parameter n11.n139 n138 = 4000;
            parameter Boolean n217 = false annotation(Evaluate = true);
            n11.n139 n218[n121] = n1.n16.n40.n36.n219.n139(n183, n206, n210, n187) if n217;
            n27.n70 n220[n121 - 1] = {n185*(n184[n98] + n184[n98 + 1])/(n187[n98] + n187[n98 + 1])*n214[n98]*n138 for n98 in 1:n121 - 1} if n217;
          protected
            parameter Boolean n207 = false annotation(Evaluate = true);
            parameter n11.n129 n208 = n27.n223(n27.n166, n27.n221, n27.n222);
            parameter Boolean n212 = false annotation(Evaluate = true);
            parameter n11.n211 n213 = n27.n164(n27.n224(n27.n166, n27.n221, n27.n222));
          equation
            if not n62 then
              n209 = n206[1:n121 - 1];
              n214 = n210[1:n121 - 1];
            elseif not n204 then
              n209 = 0.5*(n206[1:n121 - 1] + n206[2:n121]);
              n214 = 0.5*(n210[1:n121 - 1] + n210[2:n121]);
            else
              for n98 in 1:n121 - 1 loop
                n209[n98] = noEvent(if n192[n98] > 0 then n206[n98] else n206[n98 + 1]);
                n214[n98] = noEvent(if n192[n98] > 0 then n210[n98] else n210[n98 + 1]);
              end for;
            end if;
            if n205 then
              n225 = n185*{n206[n98]*n183[n98]*n183[n98]*n184[n98] - n206[n98 + 1]*n183[n98 + 1]*n183[n98 + 1]*n184[n98 + 1] for n98 in 1:n121 - 1};
            else
              n225 = zeros(n121 - 1);
            end if;
            n226 = n185*{0.5*(n184[n98] + n184[n98 + 1])*(n27.n227(n122[n98 + 1]) - n27.n227(n122[n98])) for n98 in 1:n121 - 1};
            n215 = {n228[n98]/n185*2/(n184[n98] + n184[n98 + 1]) for n98 in 1:n121 - 1};
          end n198;

          partial model n229
            parameter Boolean n230 = n67 >= n48.n49.n102 annotation(Evaluate = true);
            extends n1.n16.n40.n36.n197.n198(final n138 = 4000);
            replaceable package n231 = n1.n16.n40.n36.n231.n233 constrainedby n1.n16.n40.n36.n231.n232;
            input n11.n194 n234[n121 - 1];
            input n11.n139 n235[n121 - 1] = n138*ones(n121 - 1);
            parameter n11.n55 n236;
            parameter n11.n70 n74;
            parameter n11.n70 n75 = if n45.n73 then n45.n76*n74 else n45.n75;
          protected
            parameter n11.n55 n77(start = 1, fixed = false);
            final parameter Boolean n237 = n207 and (n212 or not n231.n238) annotation(Evaluate = true);
            final parameter Boolean n239 = (not n204) or n237 or not n62 annotation(Evaluate = true);
            n11.n194 n240[n121 - 1] = 0.5*(n187[1:n121 - 1] + n187[2:n121]);
            n11.n55 n241 = sum(n231.n242(n74/n185, n208, n208, n213, n213, n234, n240, (n184[1:n121 - 1] + n184[2:n121])/2, (n189[1:n121 - 1] + n189[2:n121])/2, n75/n185, n235));
          initial equation
            if n45.n73 then
              n77 = n241/n74*n75;
            else
              n77 = n45.n77;
            end if;
          equation
            for n98 in 1:n121 - 1 loop
              assert(n192[n98] > -n75 or n62, "assert message 962482624");
            end for;
            if n239 then
              if n230 and not n231.n243 then
                n192 = homotopy(actual = n231.n244(n215 - {n58*n191[n98]*n209[n98] for n98 in 1:n121 - 1}, n209, n209, n214, n214, n234, n240, (n184[1:n121 - 1] + n184[2:n121])/2, (n189[1:n121 - 1] + n189[2:n121])/2, n77/(n121 - 1), n235)*n185, simplified = n74/n236*(n215 - n58*n191*n208));
              else
                n215 = homotopy(actual = n231.n242(n192/n185, n209, n209, n214, n214, n234, n240, (n184[1:n121 - 1] + n184[2:n121])/2, (n189[1:n121 - 1] + n189[2:n121])/2, n75/n185, n235) + {n58*n191[n98]*n209[n98] for n98 in 1:n121 - 1}, simplified = n236/n74*n192 + n58*n191*n208);
              end if;
            else
              if n230 and not n231.n243 then
                n192 = homotopy(actual = n231.n245(n215, n206[1:n121 - 1], n206[2:n121], n210[1:n121 - 1], n210[2:n121], n234, n240, n58*n191, (n184[1:n121 - 1] + n184[2:n121])/2, (n189[1:n121 - 1] + n189[2:n121])/2, n77/(n121 - 1), n235)*n185, simplified = n74/n236*(n215 - n58*n191*n208));
              else
                n215 = homotopy(actual = n231.n246(n192/n185, n206[1:n121 - 1], n206[2:n121], n210[1:n121 - 1], n210[2:n121], n234, n240, n58*n191, (n184[1:n121 - 1] + n184[2:n121])/2, (n189[1:n121 - 1] + n189[2:n121])/2, n75/n185, n235), simplified = n236/n74*n192 + n58*n191*n208);
              end if;
            end if;
          end n229;

          model n199
            extends n1.n16.n40.n36.n197.n229(redeclare package n231 = n1.n16.n40.n36.n231.n233, n234 = n186, n236(start = 1, fixed = false), n74 = if n45.n73 then n45.n74 else 1e2*n75, n235 = n138*ones(n121 - 1));
          initial equation
            if n45.n73 then
              n236 = n241 + n58*sum(n191)*n208;
            else
              n236 = 1e3*n77;
            end if;
          end n199;
        end n197;

        package n219
          extends n1.n2.n3;

          function n139
            extends n1.n2.n247;
            input n11.n131 n248;
            input n11.n129 n249;
            input n11.n211 n250;
            input n11.n194 n251;
            output n11.n139 n252;
          algorithm
            n252 := abs(n248)*n249*n251/n250;
          end n139;
        end n219;

        package n231
          extends n1.n2.n3;

          partial package n232
            extends n1.n2.n3;
            import n1.n60.n79;
            constant Boolean n238 = true;
            constant Boolean n253 = true;
            constant Boolean n254 = true;
            constant Boolean n255 = true;
            constant Boolean n243 = false;
            constant Boolean n256 = true;

            replaceable partial function n244
              extends n1.n2.n247;
              input n11.n216 n257;
              input n11.n129 n258;
              input n11.n129 n259;
              input n11.n211 n260;
              input n11.n211 n261;
              input n11.n194 n42;
              input n11.n176 n35;
              input n11.n85 n32 = n79*n35^2/4;
              input n1.n16.n48.n196 n190 = 2.5e-5;
              input n11.n55 n77 = 1;
              input n11.n139 n138 = 4000;
              output n11.n70 n152;
            end n244;

            replaceable partial function n245
              extends n1.n2.n247;
              input n11.n216 n257;
              input n11.n129 n258;
              input n11.n129 n259;
              input n11.n211 n260;
              input n11.n211 n261;
              input n11.n194 n42;
              input n11.n176 n35;
              input Real n262(unit = "m2/s2");
              input n11.n85 n32 = n79*n35^2/4;
              input n1.n16.n48.n196 n190 = 2.5e-5;
              input n11.n55 n77 = 1;
              input n11.n139 n138 = 4000;
              output n11.n70 n152;
            end n245;

            replaceable partial function n242
              extends n1.n2.n247;
              input n11.n70 n152;
              input n11.n129 n258;
              input n11.n129 n259;
              input n11.n211 n260;
              input n11.n211 n261;
              input n11.n194 n42;
              input n11.n176 n35;
              input n11.n85 n32 = n79*n35^2/4;
              input n1.n16.n48.n196 n190 = 2.5e-5;
              input n11.n70 n75 = 0.01;
              input n11.n139 n138 = 4000;
              output n11.n216 n257;
            end n242;

            replaceable partial function n246
              extends n1.n2.n247;
              input n11.n70 n152;
              input n11.n129 n258;
              input n11.n129 n259;
              input n11.n211 n260;
              input n11.n211 n261;
              input n11.n194 n42;
              input n11.n176 n35;
              input Real n262(unit = "m2/s2");
              input n11.n85 n32 = n79*n35^2/4;
              input n1.n16.n48.n196 n190 = 2.5e-5;
              input n11.n70 n75 = 0.01;
              input n11.n139 n138 = 4000;
              output n11.n216 n257;
            end n246;
          end n232;

          package n233
            extends n232(final n238 = true, final n253 = true, final n254 = true, final n255 = true, final n256 = true);
            import n263 = n1.n264.n265;
            import n1.n264.n266;
            import n1.n264.n267;

            redeclare function extends n244
              import n1.n264;
            protected
              Real n268(min = 0) = n190/n35;
              n11.n139 n269 = min((745*n264.exp(if n268 <= 0.0065 then 1 else 0.0065/n268))^0.97, n138);
              n11.n139 n270 = n138;
              n11.n211 n250;
              n11.n129 n249;
              n11.n139 n252;
              Real n271;
              function n272 = n1.n16.n273.n162.n274.n275.n276;
            algorithm
              n249 := if n257 >= 0 then n258 else n259;
              n250 := if n257 >= 0 then n260 else n261;
              n271 := abs(n257)*2*n35^3*n249/(n42*n250*n250);
              n252 := n271/64;
              if n252 > n269 then
                n252 := -2*sqrt(n271)*n264.log10(2.51/sqrt(n271) + 0.27*n268);
                if n252 < n270 then
                  n252 := n272(n252, n269, n270, n268, n271);
                else
                end if;
              else
              end if;
              n152 := n32/n35*n250*(if n257 >= 0 then n252 else -n252);
              annotation(smoothOrder = 1);
            end n244;

            redeclare function extends n242
              import n1.n264;
              import n1.n60.n79;
            protected
              Real n268(min = 0) = n190/n35;
              n11.n139 n269 = min(745*n264.exp(if n268 <= 0.0065 then 1 else 0.0065/n268), n138);
              n11.n139 n270 = n138;
              n11.n211 n250;
              n11.n129 n249;
              n11.n139 n252;
              Real n271;
              function n272 = n1.n16.n273.n162.n274.n275.n277;
            algorithm
              n249 := if n152 >= 0 then n258 else n259;
              n250 := if n152 >= 0 then n260 else n261;
              n252 := n35*abs(n152)/(n32*n250);
              n271 := if n252 <= n269 then 64*n252 else (if n252 >= n270 then 0.25*(n252/n264.log10(n268/3.7 + 5.74/n252^0.9))^2 else n272(n252, n269, n270, n268));
              n257 := n42*n250*n250/(2*n249*n35*n35*n35)*(if n152 >= 0 then n271 else -n271);
              annotation(smoothOrder = 1);
            end n242;

            redeclare function extends n245
            protected
              Real n268(min = 0) = n190/n35;
              n11.n139 n252;
              n11.n139 n269 = min((745*exp(if n268 <= 0.0065 then 1 else 0.0065/n268))^0.97, n138);
              n11.n139 n270 = n138;
              n11.n216 n278;
              n11.n216 n279;
              n11.n70 n280;
              n11.n70 n281;
              n11.n70 n282;
              n11.n70 n283;
              n11.n216 n284 = n262*n258;
              n11.n216 n285 = n262*n259;
              n11.n70 n286 = 0;
              n11.n216 n287 = (n284 + n285)/2;
              Real n288;
            algorithm
              n278 := max(n284, n285) + n77;
              n279 := min(n284, n285) - n77;
              if n257 >= n278 then
                n152 := n289.n290(n257 - n284, n258, n259, n260, n261, n42, n35, n32, n269, n270, n268);
              elseif n257 <= n279 then
                n152 := n289.n290(n257 - n285, n258, n259, n260, n261, n42, n35, n32, n269, n270, n268);
              else
                (n280, n282) := n289.n290(n278 - n284, n258, n259, n260, n261, n42, n35, n32, n269, n270, n268);
                (n281, n283) := n289.n290(n279 - n285, n258, n259, n260, n261, n42, n35, n32, n269, n270, n268);
                (n152, n288) := n162.n291(n287, n279, n278, n281, n280, n283, n282);
                if n257 > n287 then
                  n152 := n162.n291(n257, n287, n278, n286, n280, n288, n282);
                else
                  n152 := n162.n291(n257, n279, n287, n281, n286, n283, n288);
                end if;
              end if;
              annotation(smoothOrder = 1);
            end n245;

            redeclare function extends n246
            protected
              Real n268(min = 0) = n190/n35;
              n11.n139 n269 = min(745*exp(if n268 <= 0.0065 then 1 else 0.0065/n268), n138);
              n11.n139 n270 = n138;
              n11.n70 n280;
              n11.n70 n281;
              n11.n216 n278;
              n11.n216 n279;
              n11.n216 n284 = n262*n258;
              n11.n216 n285 = n262*n259;
              Real n292;
              Real n293;
              n11.n70 n286 = 0;
              n11.n216 n287 = (n284 + n285)/2;
              Real n294;
            algorithm
              n280 := if n284 < n285 then n289.n290(n285 - n284, n258, n259, n260, n261, n42, n35, n32, n269, n270, n268) + n75 else n75;
              n281 := if n284 < n285 then n289.n290(n284 - n285, n258, n259, n260, n261, n42, n35, n32, n269, n270, n268) - n75 else -n75;
              if n152 >= n280 then
                n257 := n289.n295(n152, n258, n259, n260, n261, n42, n35, n32, n269, n270, n268) + n284;
              elseif n152 <= n281 then
                n257 := n289.n295(n152, n258, n259, n260, n261, n42, n35, n32, n269, n270, n268) + n285;
              else
                (n278, n292) := n289.n295(n280, n258, n259, n260, n261, n42, n35, n32, n269, n270, n268);
                n278 := n278 + n284;
                (n279, n293) := n289.n295(n281, n258, n259, n260, n261, n42, n35, n32, n269, n270, n268);
                n279 := n279 + n285;
                (n257, n294) := n162.n291(n286, n281, n280, n279, n278, n293, n292);
                if n152 > n286 then
                  n257 := n162.n291(n152, n286, n280, n287, n278, n294, n292);
                else
                  n257 := n162.n291(n152, n281, n286, n279, n287, n293, n294);
                end if;
              end if;
              annotation(smoothOrder = 1);
            end n246;

            package n289
              extends n1.n2.n296;

              function n290
                extends n1.n2.n247;
                input n11.n216 n297;
                input n11.n129 n258;
                input n11.n129 n259;
                input n11.n211 n260;
                input n11.n211 n261;
                input n11.n194 n42;
                input n11.n176 n35;
                input n11.n85 n32;
                input n11.n139 n269;
                input n11.n139 n270;
                input Real n268(min = 0);
                output n11.n70 n152;
                output Real n298;

              protected
                function n299
                  extends n1.n2.n247;
                  input Real n271;
                  input n11.n139 n269;
                  input n11.n139 n270;
                  input Real n268(min = 0);
                  input n11.n216 n297;
                  output n11.n139 n252;
                  output Real n300;
                protected
                  Real n301 = log10(64*n269);
                  Real n302 = log10(n269);
                  Real n303 = 1;
                  Real n304 = n268/3.7 + 5.74/n270^0.9;
                  Real n305 = log10(n304);
                  Real n306 = 0.25*(n270/n305)^2;
                  Real n307 = 2.51/sqrt(n306) + 0.27*n268;
                  Real n308 = -2*sqrt(n306)*log10(n307);
                  Real n309 = log10(n306);
                  Real n310 = log10(n308);
                  Real n311 = 0.5 + (2.51/log(10))/(n308*n307);
                  Real n312 = log10(n271);
                  Real n313;
                  Real n314;
                algorithm
                  (n313, n314) := n162.n315(n312, n301, n309, n302, n310, n303, n311);
                  n252 := 10^n313;
                  n300 := n252/abs(n297)*n314;
                  annotation(smoothOrder = 1);
                end n299;

                n11.n211 n250;
                n11.n129 n249;
                Real n271;
                n11.n139 n252;
                Real n300;
                Real n316;
                Real n304;
              algorithm
                if n297 >= 0 then
                  n249 := n258;
                  n250 := n260;
                else
                  n249 := n259;
                  n250 := n261;
                end if;
                n271 := abs(n297)*2*n35^3*n249/(n42*n250*n250);
                n316 := (2*n35^3*n249)/(n42*n250^2);
                n252 := n271/64;
                n300 := n316/64;
                if n252 > n269 then
                  n252 := -2*sqrt(n271)*log10(2.51/sqrt(n271) + 0.27*n268);
                  n304 := sqrt(n316*abs(n297));
                  n300 := 1/log(10)*(-2*log(2.51/n304 + 0.27*n268)*n316/(2*n304) + 2*2.51/(2*abs(n297)*(2.51/n304 + 0.27*n268)));
                  if n252 < n270 then
                    (n252, n300) := n299(n271, n269, n270, n268, n297);
                  else
                  end if;
                else
                end if;
                n152 := n32/n35*n250*(if n297 >= 0 then n252 else -n252);
                n298 := n32/n35*n250*n300;
                annotation(smoothOrder = 1);
              end n290;

              function n295
                extends n1.n2.n247;
                input n11.n70 n152;
                input n11.n129 n258;
                input n11.n129 n259;
                input n11.n211 n260;
                input n11.n211 n261;
                input n11.n194 n42;
                input n11.n176 n35;
                input n11.n85 n32;
                input n11.n139 n269;
                input n11.n139 n270;
                input Real n268(min = 0);
                output n11.n216 n297;
                output Real n317;

              protected
                function n272
                  extends n1.n2.n247;
                  input n11.n139 n252;
                  input n11.n139 n269;
                  input n11.n139 n270;
                  input Real n268(min = 0);
                  input n11.n70 n152;
                  output Real n271;
                  output Real n318;
                protected
                  Real n301 = log10(n269);
                  Real n302 = log10(64*n269);
                  Real n303 = 1;
                  Real n304 = n268/3.7 + 5.74/n270^0.9;
                  Real n305 = log10(n304);
                  Real n306 = 0.25*(n270/n305)^2;
                  Real n309 = log10(n270);
                  Real n310 = log10(n306);
                  Real n311 = 2 + (2*5.74*0.9)/(log(n304)*n270^0.9*n304);
                  Real n312 = log10(n252);
                  Real n313;
                  Real n314;
                algorithm
                  (n313, n314) := n162.n315(n312, n301, n309, n302, n310, n303, n311);
                  n271 := 10^n313;
                  n318 := n271/abs(n152)*n314;
                  annotation(smoothOrder = 1);
                end n272;

                n11.n211 n250;
                n11.n129 n249;
                n11.n139 n252;
                Real n271;
                Real n318;
                Real n316;
                Real n304;
              algorithm
                if n152 >= 0 then
                  n249 := n258;
                  n250 := n260;
                else
                  n249 := n259;
                  n250 := n261;
                end if;
                n252 := abs(n152)*n35/(n32*n250);
                n316 := n35/(n32*n250);
                if n252 <= n269 then
                  n271 := 64*n252;
                  n318 := 64*n316;
                elseif n252 >= n270 then
                  n271 := 0.25*(n252/log10(n268/3.7 + 5.74/n252^0.9))^2;
                  n304 := n268/3.7 + 5.74/((n316*abs(n152))^0.9);
                  n318 := 0.5*n316*n252*log(10)^2*(1/(log(n304)^2) + (5.74*0.9)/(log(n304)^3*n252^0.9*n304));
                else
                  (n271, n318) := n272(n252, n269, n270, n268, n152);
                end if;
                n297 := n42*n250*n250/(2*n249*n35*n35*n35)*(if n152 >= 0 then n271 else -n271);
                n317 := (n42*n250^2)/(2*n35^3*n249)*n318;
                annotation(smoothOrder = 1);
              end n295;
            end n289;
          end n233;
        end n231;
      end n36;
    end n40;

    package n12
      extends n1.n2.n13;

      connector n177
        replaceable package n27 = n1.n28.n12.n200 annotation(choicesAllMatching = true);
        flow n27.n70 n152;
        n27.n55 n95;
        stream n27.n319 n158;
        stream n27.n320 n159[n27.n111];
        stream n27.n321 n169[n27.n114];
      end n177;

      connector n322
        extends n177;
      end n322;

      connector n323
        extends n177;
      end n323;

      partial model n193
        import n1.n60;
        outer n1.n16.n46 n45;
        replaceable package n27 = n1.n28.n12.n200 annotation(choicesAllMatching = true);
        parameter Boolean n62 = n45.n62 annotation(Evaluate = true);
        n1.n16.n12.n322 n52(redeclare package n27 = n27, n152(min = if n62 then -n60.n7 else 0));
        n1.n16.n12.n323 n53(redeclare package n27 = n27, n152(max = if n62 then +n60.n7 else 0));
      protected
        parameter Boolean n324 = false;
        parameter Boolean n325 = false;
        parameter Boolean n326 = true;
      end n193;

      connector n327
        extends n1.n126.n118.n12.n328;
      end n327;

      partial model n172
        replaceable package n27 = n1.n28.n12.n200;
        parameter Integer n121 = 1 annotation(Evaluate = true);
        input n27.n201 n122[n121];
        input n11.n85 n92[n121];
        output n11.n329 n157[n121];
        parameter Boolean n124 = false;
        parameter n11.n331 n330 = 0 annotation(Evaluate = true);
        parameter n11.n57 n56 = n45.n56;
        outer n1.n16.n46 n45;
        n1.n16.n12.n327 n171[n121];
        n11.n57 n173[n121] = n27.n332(n122);
      equation
        if n124 then
          n157 = n171.n333 + {n330*n92[n98]*(n56 - n171[n98].n174) for n98 in 1:n121};
        else
          n157 = n171.n333;
        end if;
      end n172;

      partial model n104
        import n1.n16.n48;
        import n1.n16.n48.n49;
        import n1.n28.n12.n334.n335;
        outer n1.n16.n46 n45;
        replaceable package n27 = n1.n28.n12.n200 annotation(choicesAllMatching = true);
        input n11.n84 n87;
        parameter n48.n49 n47 = n45.n47 annotation(Evaluate = true);
        parameter n48.n49 n64 = n45.n64 annotation(Evaluate = true);
        final parameter n48.n49 n65 = n64 annotation(Evaluate = true);
        final parameter n48.n49 n66 = n64 annotation(Evaluate = true);
        parameter n27.n55 n71 = n45.n71;
        parameter Boolean n336 = true annotation(Evaluate = true);
        parameter n27.n57 n72 = if n336 then n45.n72 else n27.n339(n71, n337, n338);
        parameter n27.n319 n337 = if n336 then n27.n340(n71, n72, n338) else n27.n341;
        parameter n27.n320 n338[n27.n342] = n27.n222;
        parameter n27.n321 n343[n27.n114](quantity = n27.n344) = n27.n345;
        n27.n346 n94(n347 = (if n47 == n49.n68 and n64 == n49.n68 then false else true), n95(start = n71), n167(start = n337), n174(start = n72), n168(start = n338[1:n27.n111]));
        n11.n349 n348;
        n11.n350 n203;
        n11.n350 n351[n27.n111];
        n11.n350 n352[n27.n114];
        n27.n321 n170[n27.n114];
        n11.n70 n151;
        n11.n70 n153[n27.n111];
        n27.n115 n154[n27.n114];
        n11.n109 n155;
        n11.n329 n156;
        n11.n353 n97;
      protected
        parameter Boolean n93 = not n27.n96;
        Real n354[n27.n114](min = fill(n1.n60.n5, n27.n114));
      initial equation
        if n47 == n49.n50 then
          if n27.n357 == n335.n358 or n27.n357 == n335.n359 then
            n94.n167 = n337;
          else
            n94.n174 = n72;
          end if;
        elseif n47 == n49.n102 then
          if n27.n357 == n335.n358 or n27.n357 == n335.n359 then
            der(n94.n167) = 0;
          else
            der(n94.n174) = 0;
          end if;
        end if;
        if n64 == n49.n50 then
          if n93 then
            n94.n95 = n71;
          end if;
        elseif n64 == n49.n102 then
          if n93 then
            der(n94.n95) = 0;
          end if;
        end if;
        if n65 == n49.n50 then
          n94.n168 = n338[1:n27.n111];
        elseif n65 == n49.n102 then
          der(n94.n168) = zeros(n27.n111);
        end if;
        if n66 == n49.n50 then
          n354 = n203*n343[1:n27.n114]./n27.n356;
        elseif n66 == n49.n102 then
          der(n354) = zeros(n27.n114);
        end if;
      equation
        assert(not (n47 <> n49.n68 and n64 == n49.n68) or n27.n96, "assert message 1243918012");
        n203 = n87*n94.n101;
        n351 = n203*n94.n168;
        n348 = n203*n94.n355;
        n352 = n203*n170;
        if n47 == n49.n68 then
          0 = n155 + n156 + n97;
        else
          der(n348) = n155 + n156 + n97;
        end if;
        if n64 == n49.n68 then
          0 = n151;
        else
          der(n203) = n151;
        end if;
        if n65 == n49.n68 then
          zeros(n27.n111) = n153;
        else
          der(n351) = n153;
        end if;
        if n66 == n49.n68 then
          zeros(n27.n114) = n154;
        else
          der(n354) = n154./n27.n356;
        end if;
        n352 = n354.*n27.n356;
      end n104;

      partial model n202
        outer n1.n16.n46 n45;
        replaceable package n27 = n1.n28.n12.n200;
        parameter Boolean n62 = n45.n62 annotation(Evaluate = true);
        parameter Integer n203 = 1;
        input n11.n194 n186[n203];
        n27.n70 n192[n203](each min = if n62 then -n1.n60.n7 else 0, each start = n69, each stateSelect = if n67 == n48.n49.n68 then StateSelect.default else StateSelect.prefer);
        parameter n1.n16.n48.n49 n67 = n45.n67 annotation(Evaluate = true);
        parameter n27.n70 n69 = n45.n69;
        n11.n361 n360[n203];
        n11.n362 n225[n203];
        n11.n362 n226[n203];
        n11.n362 n228[n203];
      initial equation
        if n67 == n48.n49.n50 then
          n192 = fill(n69, n203);
        elseif n67 == n48.n49.n102 then
          der(n192) = zeros(n203);
        end if;
      equation
        n360 = {n192[n98]*n186[n98] for n98 in 1:n203};
        if n67 == n48.n49.n68 then
          zeros(n203) = n225 - n226 - n228;
        else
          der(n360) = n225 - n226 - n228;
        end if;
      end n202;
    end n12;

    package n48
      extends n1.n2.n363;
      type n196 = n1.n2.n364(final quantity = "Length", final unit = "m", displayUnit = "mm", min = 0);
      type n49 = enumeration(n63, n50, n102, n68);
    end n48;

    package n273
      extends n1.n2.n103;
      import n365 = n1.n60.n79;
      import n366 = n1.n16.n273.n162.n367;
      import n368 = n1.n16.n273.n162.n48;

      package n162
        extends n1.n2.n369;

        package n274
          extends n1.n2.n370;

          package n275
            extends n1.n2.n370;

            function n276
              extends n1.n2.n247;
              import n1.n264;
              input Real n138;
              input n11.n139 n269;
              input n11.n139 n270;
              input Real n268;
              input Real n271;
              output n11.n139 n252;
            protected
              Real n301 = n264.log10(64*n269);
              Real n302 = n264.log10(n269);
              Real n371 = 1;
              Real n316 = (0.5/n264.log(10))*5.74*0.9;
              Real n304 = n268/3.7 + 5.74/n270^0.9;
              Real n305 = n264.log10(n304);
              Real n306 = 0.25*(n270/n305)^2;
              Real n307 = 2.51/sqrt(n306) + 0.27*n268;
              Real n308 = -2*sqrt(n306)*n264.log10(n307);
              Real n309 = n264.log10(n306);
              Real n310 = n264.log10(n308);
              Real n372 = 0.5 + (2.51/n264.log(10))/(n308*n307);
              Real n373 = n309 - n301;
              Real n203 = (n310 - n302)/n373;
              Real n374 = (3*n203 - 2*n371 - n372)/n373;
              Real n375 = (n371 + n372 - 2*n203)/(n373*n373);
              Real n376 = 64*n269;
              Real n377 = n264.log10(n271/n376);
            algorithm
              n252 := n269*(n271/n376)^(n371 + n377*(n374 + n377*n375));
              annotation(Inline = false, smoothOrder = 5);
            end n276;

            function n277
              extends n1.n2.n247;
              import n1.n264;
              input n11.n139 n252;
              input n11.n139 n269;
              input n11.n139 n270;
              input Real n268;
              output Real n271;
            protected
              Real n301 = n264.log10(n269);
              Real n302 = n264.log10(64*n269);
              Real n371 = 1;
              Real n316 = (0.5/n264.log(10))*5.74*0.9;
              Real n304 = n268/3.7 + 5.74/n270^0.9;
              Real n305 = n264.log10(n304);
              Real n306 = 0.25*(n270/n305)^2;
              Real n307 = 2.51/sqrt(n306) + 0.27*n268;
              Real n308 = -2*sqrt(n306)*n264.log10(n307);
              Real n309 = n264.log10(n270);
              Real n310 = n264.log10(n306);
              Real n372 = 2 + 4*n316/(n304*n305*(n270)^0.9);
              Real n373 = n309 - n301;
              Real n203 = (n310 - n302)/n373;
              Real n374 = (3*n203 - 2*n371 - n372)/n373;
              Real n375 = (n371 + n372 - 2*n203)/(n373*n373);
              Real n377 = n264.log10(n252/n269);
            algorithm
              n271 := 64*n269*(n252/n269)^(n371 + n377*(n374 + n377*n375));
              annotation(Inline = false, smoothOrder = 5);
            end n277;
          end n275;
        end n274;
      end n162;
    end n273;

    package n162
      extends n1.n2.n369;

      function n165
        extends n1.n2.n247;
        input Real n312;
        input Real n378(min = 0) = 0.01;
        input Real n379(min = 0) = 1;
        input Real n380(min = 0) = 1;
        input Boolean n381 = false;
        input Real n382(min = 0) = 1;
        output Real n313;

      protected
        encapsulated function n383
          import n1;
          extends n1.n2.n247;
          import n1.n16.n162.n384;
          input Real n312;
          input Real n301;
          input Real n379;
          input Real n380;
          input Boolean n381 = false;
          input Real n382(min = 0) = 1;
          output Real n313;
        protected
          Real n309;
          Real n302;
          Real n310;
          Real n303;
          Real n311;
          Real n385;
          Real n386;
          Real n387;
          Real n388;
          Real n389;
        algorithm
          n309 := -n301;
          if n312 <= n309 then
            n313 := -n380*n312^2;
          else
            n302 := n379*n301^2;
            n310 := -n380*n309^2;
            n303 := n379*2*n301;
            n311 := -n380*2*n309;
            if n381 then
              n388 := n382;
            else
              n385 := n309/n301;
              n388 := ((3*n310 - n309*n311)/n385 - (3*n302 - n301*n303)*n385)/(2*n301*(1 - n385));
            end if;
            n386 := sqrt(5)*n379*n301;
            n387 := sqrt(5)*n380*abs(n309);
            n389 := 0.9*(if n386 < n387 then n386 else n387);
            if n389 < n388 then
              n388 := n389;
            else
            end if;
            n313 := if n312 >= 0 then n384(n312, n301, n302, n303, n388) else n384(n312, n309, n310, n311, n388);
          end if;
          annotation(smoothOrder = 2);
        end n383;
      algorithm
        n313 := smooth(2, if n312 >= n378 then n379*n312^2 else if n312 <= -n378 then -n380*n312^2 else if n379 >= n380 then n383(n312, n378, n379, n380, n381, n382) else -n383(-n312, n378, n380, n379, n381, n382));
        annotation(smoothOrder = 2);
      end n165;

      function n163
        extends n1.n2.n247;
        input Real n312;
        input Real n302;
        input Real n310;
        input Real n378(min = 0) = 1e-5;
        output Real n313;
      algorithm
        n313 := smooth(1, if n312 > n378 then n302 else if n312 < -n378 then n310 else if n378 > 0 then (n312/n378)*((n312/n378)^2 - 3)*(n310 - n302)/4 + (n302 + n310)/2 else (n302 + n310)/2);
      end n163;

      function n384
        extends n1.n2.n247;
        input Real n312;
        input Real n301;
        input Real n302;
        input Real n303;
        input Real n388;
        output Real n313;
      protected
        Real n390;
        Real n391;
        Real n392;
        Real n393;
      algorithm
        n390 := n301*n388;
        n391 := 3*n302 - n301*n303 - 2*n390;
        n392 := n302 - n391 - n390;
        n393 := n312/n301;
        n313 := n393*(n390 + n393*(n391 + n393*n392));
        annotation(smoothOrder = 3);
      end n384;

      function n291
        extends n1.n2.n247;
        input Real n312;
        input Real n394;
        input Real n301;
        input Real n395;
        input Real n302;
        input Real n388;
        input Real n303;
        output Real n313;
        output Real n396;
      protected
        Real n397;
        Real n398;
        Real n399;
        Real n250;
        Real n400;
        Real n401;
        Real n249;
        Real n402;
        Real n403;
        Real n404;
        Real n405;
        Real n406;
        Real n390;
        Real n391;
        Real n407;
        Real n408;
        Real n409;
        Real n410;
        Boolean n411 = false;
      algorithm
        assert(n394 < n301, "assert message 1698541223");
        if n388*n303 >= 0 then
        else
          assert(abs(n388) < n1.n60.n5 or abs(n303) < n1.n60.n5, "assert message 1220274287");
        end if;
        n397 := n301 - n394;
        n398 := (n302 - n395)/n397;
        if abs(n398) <= 0 then
          n313 := n395 + n398*(n312 - n394);
          n396 := 0;
        elseif abs(n303 + n388 - 2*n398) < 100*n1.n60.n5 then
          n313 := n395 + (n312 - n394)*(n388 + (n312 - n394)/n397*((-2*n388 - n303 + 3*n398) + (n312 - n394)*(n388 + n303 - 2*n398)/n397));
          n409 := (n394 + n301)/2;
          n396 := 3*(n388 + n303 - 2*n398)*(n409 - n394)^2/n397^2 + 2*(-2*n388 - n303 + 3*n398)*(n409 - n394)/n397 + n388;
        else
          n399 := 1/3*(-3*n394*n388 - 3*n394*n303 + 6*n394*n398 - 2*n397*n388 - n397*n303 + 3*n397*n398)/(-n388 - n303 + 2*n398);
          n250 := n399 - n394;
          n400 := n301 - n399;
          n401 := 3*(n388 + n303 - 2*n398)*(n399 - n394)^2/n397^2 + 2*(-2*n388 - n303 + 3*n398)*(n399 - n394)/n397 + n388;
          n409 := 0.25*sign(n398)*min(abs(n401), abs(n398));
          if abs(n388 - n303) <= 100*n1.n60.n5 then
            n410 := n388;
            if n302 > n395 + n388*(n301 - n394) then
              n411 := true;
            else
            end if;
          elseif abs(n303 + n388 - 2*n398) < 100*n1.n60.n5 then
            n410 := (6*n398*(n303 + n388 - 3/2*n398) - n303*n388 - n303^2 - n388^2)*(if (n303 + n388 - 2*n398) >= 0 then 1 else -1)*n1.n60.n7;
          else
            n410 := (6*n398*(n303 + n388 - 3/2*n398) - n303*n388 - n303^2 - n388^2)/(3*(n303 + n388 - 2*n398));
          end if;
          if (((n250 > 0) and (n400 < n397) and (n398*n401 <= 0)) or (abs(n409) < abs(n410) and n410*n398 >= 0) or (abs(n409) < abs(0.1*n398))) and not n411 then
            n396 := n409;
            if abs(n396) < abs(n410) and n410*n398 >= 0 then
              n396 := n410;
            else
            end if;
            if abs(n396) < abs(0.1*n398) then
              n396 := 0.1*n398;
            else
            end if;
            n402 := (n388*n250 + n303*n400)/n397;
            if abs(n402 - n396) < 1e-6 then
              n396 := (1 - 1e-6)*n402;
            else
            end if;
            n249 := 3*(n398 - n396)/(n402 - n396);
            n403 := n249*n250;
            n404 := n249*n400;
            n405 := n394 + n403;
            n406 := n301 - n404;
            n390 := (n388 - n396)/max(n403^2, 100*n1.n60.n5);
            n391 := (n303 - n396)/max(n404^2, 100*n1.n60.n5);
            n407 := n395 - n390/3*(n394 - n405)^3 - n396*n394;
            n408 := n302 - n391/3*(n301 - n406)^3 - n396*n301;
            if (n312 < n405) then
              n313 := n390/3*(n312 - n405)^3 + n396*n312 + n407;
            elseif (n312 < n406) then
              n313 := n396*n312 + n407;
            else
              n313 := n391/3*(n312 - n406)^3 + n396*n312 + n408;
            end if;
          else
            n313 := n395 + (n312 - n394)*(n388 + (n312 - n394)/n397*((-2*n388 - n303 + 3*n398) + (n312 - n394)*(n388 + n303 - 2*n398)/n397));
            n409 := (n394 + n301)/2;
            n396 := 3*(n388 + n303 - 2*n398)*(n409 - n394)^2/n397^2 + 2*(-2*n388 - n303 + 3*n398)*(n409 - n394)/n397 + n388;
          end if;
        end if;
        annotation(smoothOrder = 1);
      end n291;

      function n315
        extends n1.n2.n247;
        input Real n312;
        input Real n301;
        input Real n309;
        input Real n302;
        input Real n310;
        input Real n303;
        input Real n311;
        output Real n313;
        output Real n314;
      protected
        Real n167;
        Real n412;
        Real n413;
        Real n414;
        Real n415;
        Real n416;
        Real n417;
        Real n418;
        Real n419;
        Real n420;
        Real n305;
        Real n304;
      algorithm
        n167 := n309 - n301;
        if abs(n167) > 0 then
          n412 := (n312 - n301)/n167;
          n305 := n412^3;
          n304 := n412^2;
          n413 := 2*n305 - 3*n304 + 1;
          n414 := n305 - 2*n304 + n412;
          n415 := -2*n305 + 3*n304;
          n416 := n305 - n304;
          n417 := 6*(n304 - n412);
          n418 := 3*n304 - 4*n412 + 1;
          n419 := 6*(n412 - n304);
          n420 := 3*n304 - 2*n412;
          n313 := n302*n413 + n167*n303*n414 + n310*n415 + n167*n311*n416;
          n314 := n302*n417/n167 + n303*n418 + n310*n419/n167 + n311*n420;
        else
          n313 := (n302 + n310)/2;
          n314 := sign(n310 - n302)*n1.n60.n7;
        end if;
        annotation(smoothOrder = 3);
      end n315;
    end n162;
  end n16;

  package n28
    extends n1.n2.n3;
    import n1.n10.n11;
    import n17 = n1.n10.n18;

    package n12
      extends n1.n2.n13;

      partial package n200
        extends n1.n28.n12.n48;
        extends n1.n2.n421;
        constant n1.n28.n12.n334.n335 n357;
        constant String n422 = "unusablePartialMedium";
        constant String n423[:] = {n422};
        constant String n344[:] = fill("", 0);
        constant Boolean n96;
        constant Boolean n424 = true;
        constant Boolean n425 = false;
        constant n57 n426 = 298.15;
        constant n320 n427[n342] = fill(1/n342, n342);
        constant n55 n166 = 101325;
        constant n57 n221 = n1.n10.n18.n428(20);
        constant n319 n341 = n340(n166, n221, n222);
        constant n320 n222[n342] = n427;
        constant n321 n345[n114] = fill(0, n114);
        final constant Integer n429 = size(n423, 1);
        constant Integer n342 = n429;
        constant Integer n111 = if n425 then 0 else if n424 then n429 - 1 else n429;
        final constant Integer n114 = size(n344, 1);
        constant Real n356[n114](min = fill(n1.n60.n5, n114)) = 1.0e-6*ones(n114);
        replaceable record n430 = n1.n28.n12.n48.n431.n430;

        replaceable record n201
          extends n1.n2.n175;
        end n201;

        replaceable partial model n346
          n432 n95;
          n433 n168[n111](start = n427[1:n111]);
          n434 n167;
          n129 n101;
          n57 n174;
          n320 n435[n342](start = n427);
          n436 n355;
          n438 n437;
          n440 n439;
          n201 n123;
          parameter Boolean n347 = false annotation(Evaluate = true);
          parameter Boolean n441 = true;
          n1.n10.n443.n444 n442 = n1.n10.n18.n445(n174);
          n1.n10.n443.n447 n446 = n1.n10.n18.n448(n95);
          connector n432 = input n11.n55;
          connector n434 = input n11.n319;
          connector n433 = input n11.n320;
        equation
          if n441 then
            n168 = n435[1:n111];
            if n425 then
              n435 = n427;
            end if;
            if n424 and not n425 then
              n435[n342] = 1 - sum(n168);
            end if;
            for n98 in 1:n342 loop
              assert(n435[n98] >= -1.e-5 and n435[n98] <= 1 + 1.e-5, "assert message 1981392411");
            end for;
          end if;
          assert(n95 >= 0.0, "assert message 830749431");
        end n346;

        replaceable partial function n224
          extends n1.n2.n247;
          input n55 n95;
          input n57 n174;
          input n320 n435[:] = n427;
          output n201 n123;
        end n224;

        replaceable partial function n160
          extends n1.n2.n247;
          input n55 n95;
          input n319 n167;
          input n320 n435[:] = n427;
          output n201 n123;
        end n160;

        replaceable partial function n449
          extends n1.n2.n247;
          input Real n312;
          input n201 n450;
          input n201 n451;
          input Real n378(min = 0);
          output n201 n123;
        end n449;

        replaceable partial function n164
          extends n1.n2.n247;
          input n201 n123;
          output n211 n400;
        end n164;

        replaceable partial function n452
          extends n1.n2.n247;
          input n201 n123;
          output n454 n453;
        end n452;

        replaceable partial function n227
          extends n1.n2.n247;
          input n201 n123;
          output n55 n95;
        end n227;

        replaceable partial function n332
          extends n1.n2.n247;
          input n201 n123;
          output n57 n174;
        end n332;

        replaceable partial function n161
          extends n1.n2.n247;
          input n201 n123;
          output n129 n101;
        end n161;

        replaceable partial function n455
          extends n1.n2.n247;
          input n201 n123;
          output n319 n167;
        end n455;

        replaceable partial function n456
          extends n1.n2.n247;
          input n201 n123;
          output n457 n355;
        end n456;

        replaceable partial function n458
          extends n1.n2.n247;
          input n201 n123;
          output n459 n134;
        end n458;

        replaceable partial function n460
          extends n1.n2.n247;
          input n201 n123;
          output n457 n58;
        end n460;

        replaceable partial function n461
          extends n1.n2.n247;
          input n201 n123;
          output n457 n462;
        end n461;

        replaceable partial function n463
          extends n1.n2.n247;
          input n201 n123;
          output n438 n464;
        end n463;

        replaceable partial function n465
          extends n1.n2.n247;
          input n201 n123;
          output n438 n466;
        end n465;

        replaceable partial function n467
          extends n1.n2.n247;
          input n201 n123;
          output n469 n468;
        end n467;

        replaceable partial function n470
          extends n1.n2.n247;
          input n55 n471;
          input n201 n472;
          output n319 n473;
        end n470;

        replaceable partial function n474
          extends n1.n2.n247;
          input n201 n123;
          output n476 n475;
        end n474;

        replaceable partial function n477
          extends n1.n2.n247;
          input n201 n123;
          output n479 n478;
        end n477;

        replaceable partial function n480
          extends n1.n2.n247;
          input n201 n123;
          output n11.n482 n481;
        end n480;

        replaceable partial function n483
          extends n1.n2.n247;
          input n201 n123;
          output n485 n484;
        end n483;

        replaceable partial function n486
          extends n1.n2.n247;
          input n201 n123;
          output n488 n487;
        end n486;

        replaceable partial function n489
          extends n1.n2.n247;
          input n201 n123;
          output n129 n490[n342];
        end n489;

        replaceable partial function n491
          extends n1.n2.n247;
          input n201 n123;
          output n440 n439;
        end n491;

        replaceable function n340
          extends n1.n2.n247;
          input n55 n95;
          input n57 n174;
          input n320 n435[:] = n427;
          output n319 n167;
        algorithm
          n167 := n455(n224(n95, n174, n435));
          annotation(inverse(n174 = n339(n95, n167, n435)));
        end n340;

        replaceable function n223
          extends n1.n2.n247;
          input n55 n95;
          input n57 n174;
          input n320 n435[:];
          output n129 n101;
        algorithm
          n101 := n161(n224(n95, n174, n435));
        end n223;

        replaceable function n339
          extends n1.n2.n247;
          input n55 n95;
          input n319 n167;
          input n320 n435[:] = n427;
          output n57 n174;
        algorithm
          n174 := n332(n160(n95, n167, n435));
        end n339;

        type n70 = n11.n70(quantity = "MassFlowRate." + n422, min = -1.0e5, max = 1.e5);
      end n200;

      partial package n492
        extends n200(final n424 = true, final n425 = true);

        redeclare replaceable partial model extends n346(final n441 = true) end n346;
      end n492;

      partial package n493
        extends n12.n492(final n357 = n1.n28.n12.n334.n335.n494, final n96 = true);
        constant n438 n495;
        constant n438 n496;
        constant n129 n497;
        constant n211 n498;
        constant n454 n499;
        constant n476 n500;
        constant n57 n501;
        constant n57 n502;
        constant n57 n503 = n426;
        constant n440 n504;
        constant n430 n505[n429];

        redeclare record extends n201
          n55 n95;
          n57 n174;
        end n201;

        redeclare replaceable model extends n346(n174(stateSelect = if n347 then StateSelect.prefer else StateSelect.default), n95(stateSelect = if n347 then StateSelect.prefer else StateSelect.default))
        equation
          assert(n174 >= n501 and n174 <= n502, "assert message 176042410");
          n167 = n340(n95, n174, n435);
          n355 = n496*(n174 - n503);
          n101 = n497;
          n437 = 0;
          n439 = n504;
          n123.n174 = n174;
          n123.n95 = n95;
        end n346;

        redeclare function n224
          extends n1.n2.n247;
          input n55 n95;
          input n57 n174;
          input n320 n435[:] = n427;
          output n201 n123;
        algorithm
          n123 := n201(n95 = n95, n174 = n174);
        end n224;

        redeclare function n160
          extends n1.n2.n247;
          input n55 n95;
          input n319 n167;
          input n320 n435[:] = n427;
          output n201 n123;
        algorithm
          n123 := n201(n95 = n95, n174 = n503 + n167/n495);
        end n160;

        redeclare function extends n449
        algorithm
          n123 := n201(n95 = n28.n506.n507(n312, n450.n95, n451.n95, n378), n174 = n28.n506.n507(n312, n450.n174, n451.n174, n378));
        end n449;

        redeclare function extends n164
        algorithm
          n400 := n498;
        end n164;

        redeclare function extends n452
        algorithm
          n453 := n499;
        end n452;

        redeclare function extends n227
        algorithm
          n95 := n123.n95;
        end n227;

        redeclare function extends n332
        algorithm
          n174 := n123.n174;
        end n332;

        redeclare function extends n161
        algorithm
          n101 := n497;
        end n161;

        redeclare function extends n455
        algorithm
          n167 := n495*(n123.n174 - n503);
        end n455;

        redeclare function extends n463
        algorithm
          n464 := n495;
        end n463;

        redeclare function extends n465
        algorithm
          n466 := n496;
        end n465;

        redeclare function extends n467
        algorithm
          n468 := n495/n496;
        end n467;

        redeclare function extends n474
        algorithm
          n475 := n500;
        end n474;

        redeclare function n340
          extends n1.n2.n247;
          input n55 n95;
          input n57 n174;
          input n320 n435[n342];
          output n319 n167;
        algorithm
          n167 := n495*(n174 - n503);
        end n340;

        redeclare function n339
          extends n1.n2.n247;
          input n55 n95;
          input n319 n167;
          input n320 n435[n342];
          output n57 n174;
        algorithm
          n174 := n503 + n167/n495;
        end n339;

        redeclare function extends n456
          extends n1.n2.n247;
        algorithm
          n355 := n496*(n123.n174 - n503);
        end n456;

        redeclare function extends n458
          extends n1.n2.n247;
        algorithm
          n134 := n496*n1.n264.log(n123.n174/n503);
        end n458;

        redeclare function extends n460
          extends n1.n2.n247;
        algorithm
          n58 := n455(n123) - n123.n174*n458(n123);
        end n460;

        redeclare function extends n461
          extends n1.n2.n247;
        algorithm
          n462 := n456(n123) - n123.n174*n458(n123);
        end n461;

        redeclare function extends n470
        algorithm
          n473 := n495*(n332(n472) - n503);
        end n470;

        redeclare function extends n477
        algorithm
          n478 := 0.0;
        end n477;

        redeclare function extends n480
        algorithm
          n481 := 0;
        end n480;

        redeclare function extends n483
        algorithm
          n484 := 0;
        end n483;

        redeclare function extends n486
        algorithm
          n487 := 0;
        end n486;

        redeclare function extends n489
        algorithm
          n490 := fill(0, n342);
        end n489;

        redeclare function extends n491
        algorithm
          n439 := n504;
        end n491;
      end n493;

      package n334
        extends n1.n2.n3;
        type n335 = enumeration(n174, n494, n358, n359, n508, n509);
      end n334;

      package n48
        extends n1.n2.n3;
        type n55 = n11.n55(min = 0, max = 1.e8, nominal = 1.e5, start = 1.e5);
        type n129 = n11.n129(min = 0, max = 1.e5, nominal = 1, start = 1);
        type n211 = n11.n211(min = 0, max = 1.e8, nominal = 1.e-3, start = 1.e-3);
        type n109 = n11.n109(nominal = 1000.0, min = -1.0e8, max = 1.e8);
        type n320 = Real(quantity = "MassFraction", final unit = "kg/kg", min = 0, max = 1, nominal = 0.1);
        type n440 = n11.n440(min = 0.001, max = 0.25, nominal = 0.032);
        type n510 = n11.n510(min = 1e-6, max = 1.0e6, nominal = 1.0);
        type n469 = n11.n511(min = 1, max = 500000, nominal = 1.2, start = 1.2);
        type n457 = n11.n457(min = -1.0e8, max = 1.e8, nominal = 1.e6);
        type n436 = n457;
        type n319 = n11.n319(min = -1.0e10, max = 1.e10, nominal = 1.e6);
        type n459 = n11.n459(min = -1.e7, max = 1.e7, nominal = 1.e3);
        type n438 = n11.n438(min = 0, max = 1.e7, nominal = 1.e3, start = 1.e3);
        type n57 = n11.n57(min = 1, max = 1.e4, nominal = 300, start = 288.15);
        type n454 = n11.n454(min = 0, max = 500, nominal = 1, start = 1);
        type n476 = n11.n131(min = 0, max = 1.e5, nominal = 1000, start = 1000);
        type n321 = Real(min = 0.0, start = 1.0);
        type n115 = Real(unit = "kg/s");
        type n479 = Real(min = 0, max = 1.0e8, unit = "1/K");
        type n512 = Real(min = 0.0, max = 2.0, unit = "debye", quantity = "ElectricDipoleMoment");
        type n485 = n11.n485;
        type n488 = n11.n488;

        package n431
          extends n2.n3;

          record n430
            extends n1.n2.n175;
            String n513;
            String n514;
            String n515;
            String n516;
            n440 n491;
          end n430;
        end n431;

        package n517
          extends n2.n3;

          record n430
            extends n1.n28.n12.n48.n431.n430;
            n57 n518;
            n55 n519;
            n510 n520;
            Real n521;
            n57 n522;
            n55 n523;
            n57 n524;
            n57 n525;
            n512 n526;
            Boolean n527 = false;
            Boolean n528 = false;
            Boolean n529 = false;
            Boolean n530 = false;
            Boolean n531 = false;
            Boolean n532 = false;
            Boolean n533 = false;
            Boolean n534 = false;
            Boolean n535 = false;
            Boolean n536 = false;
            n319 n537 = 0.0;
            n459 n538 = 0.0;
            n319 n539 = 0.0;
            n459 n540 = 0.0;
          end n430;
        end n517;
      end n48;
    end n12;

    package n506
      extends n1.n2.n3;
      constant Real n541 = 1.0e-9;

      function n507
        extends n1.n2.n247;
        input Real n312;
        input Real n302;
        input Real n310;
        input Real n378(min = 0) = 1e-5;
        output Real n313;
      algorithm
        n313 := smooth(1, if n312 > n378 then n302 else if n312 < -n378 then n310 else if abs(n378) > 0 then (n312/n378)*((n312/n378)^2 - 3)*(n310 - n302)/4 + (n302 + n310)/2 else (n302 + n310)/2);
        annotation(Inline = true, smoothOrder = 1);
      end n507;
    end n506;

    package n29
      extends n1.n2.n78;
      import n1.n28.n29.n30.n542;

      package n30
        constant n1.n28.n12.n48.n431.n430 n542[1](each n515 = "H2O", each n516 = "H2O", each n514 = "7732-18-5", each n513 = "oxidane", each n491 = 0.018015268);
        extends n12.n493(n422 = "SimpleLiquidWater", n495 = 4184, n496 = 4184, n497 = 995.586, n498 = 1.e-3, n499 = 0.598, n500 = 1484, n501 = n17.n428(-1), n502 = n17.n428(130), n503 = 273.15, n504 = 0.018015268, n505 = n542);
      end n30;
    end n29;
  end n28;

  package n126
    extends n1.n2.n3;
    import n1.n10.n11;

    package n118
      extends n1.n2.n3;

      package n12
        extends n1.n2.n13;

        partial connector n328
          n11.n57 n174;
          flow n11.n329 n333;
        end n328;

        connector n127
          extends n328;
        end n127;
      end n12;
    end n118;
  end n126;

  package n264
    extends n1.n2.n3;

    package n2
      extends n1.n2.n543;

      partial function n544 end n544;

      partial function n545 end n545;
    end n2;

    function asin
      extends n1.n264.n2.n545;
      input Real n355;
      output n1.n10.n11.n546 n313;
      external "builtin" n313 = asin(n355);
    end asin;

    function exp
      extends n1.n264.n2.n545;
      input Real n355;
      output Real n313;
      external "builtin" n313 = exp(n355);
    end exp;

    function log
      extends n1.n264.n2.n544;
      input Real n355;
      output Real n313;
      external "builtin" n313 = log(n355);
    end log;

    function log10
      extends n1.n264.n2.n544;
      input Real n355;
      output Real n313;
      external "builtin" n313 = log10(n355);
    end log10;
  end n264;

  package n60
    extends n1.n2.n3;
    import n1.n10.n11;
    import n1.n10.n443;
    final constant Real n79 = 2*n1.n264.asin(1.0);
    final constant Real n5 = n0.n4.n5;
    final constant Real n7 = n0.n4.n7;
    final constant n11.n131 n396 = 299792458;
    final constant n11.n59 n61 = 9.80665;
    final constant n11.n548 n547 = 1.602176634e-19;
    final constant Real n167(final unit = "J.s") = 6.62607015e-34;
    final constant Real n330(final unit = "J/K") = 1.380649e-23;
    final constant Real n549(final unit = "1/mol") = 6.02214076e23;
    final constant Real n550(final unit = "N/A2") = 4*n79*1.00000000055e-7;
    final constant n443.n444 n551 = -273.15;
  end n60;

  package n2
    extends n2.n3;

    partial package n20
      extends n1.n2.n3;
    end n20;

    partial model n23 end n23;

    partial package n3 end n3;

    partial package n103
      extends n1.n2.n3;
    end n103;

    partial package n78
      extends n1.n2.n3;
    end n78;

    partial package n13
      extends n1.n2.n3;
    end n13;

    partial package n369
      extends n1.n2.n3;
    end n369;

    partial package n363
      extends n1.n2.n3;
    end n363;

    partial package n370
      extends n1.n2.n3;
    end n370;

    partial package n543
      extends n1.n2.n3;
    end n543;

    partial package n296 end n296;

    partial package n421
      extends n1.n2.n3;
    end n421;

    partial function n247 end n247;

    partial record n175 end n175;

    type n364
      extends Real;
    end n364;
  end n2;

  package n10
    extends n1.n2.n3;

    package n11
      extends n1.n2.n3;
      type n546 = Real(final quantity = "Angle", final unit = "rad", displayUnit = "deg");
      type n194 = Real(final quantity = "Length", final unit = "m");
      type n81 = n194(min = 0);
      type n176 = n194(min = 0);
      type n85 = Real(final quantity = "Area", final unit = "m2");
      type n84 = Real(final quantity = "Volume", final unit = "m3");
      type n131 = Real(final quantity = "Velocity", final unit = "m/s");
      type n59 = Real(final quantity = "Acceleration", final unit = "m/s2");
      type n350 = Real(quantity = "Mass", final unit = "kg", min = 0);
      type n129 = Real(final quantity = "Density", final unit = "kg/m3", displayUnit = "g/cm3", min = 0.0);
      type n361 = Real(final quantity = "Momentum", final unit = "kg.m/s");
      type n362 = Real(final quantity = "Force", final unit = "N");
      type n216 = Real(final quantity = "Pressure", final unit = "Pa", displayUnit = "bar");
      type n55 = n216(min = 0.0, nominal = 1e5);
      type n211 = Real(final quantity = "DynamicViscosity", final unit = "Pa.s", min = 0);
      type n349 = Real(final quantity = "Energy", final unit = "J");
      type n353 = Real(final quantity = "Power", final unit = "W");
      type n133 = n353;
      type n109 = Real(final quantity = "EnthalpyFlowRate", final unit = "W");
      type n70 = Real(quantity = "MassFlowRate", final unit = "kg/s");
      type n552 = Real(final quantity = "MomentumFlux", final unit = "N");
      type n553 = Real(final quantity = "ThermodynamicTemperature", final unit = "K", min = 0.0, start = 288.15, nominal = 300, displayUnit = "degC") annotation(absoluteValue = true);
      type n57 = n553;
      type n554 = Real(final quantity = "Compressibility", final unit = "1/Pa");
      type n482 = n554;
      type n329 = Real(final quantity = "Power", final unit = "W");
      type n454 = Real(final quantity = "ThermalConductivity", final unit = "W/(m.K)");
      type n331 = Real(final quantity = "CoefficientOfHeatTransfer", final unit = "W/(m2.K)");
      type n438 = Real(final quantity = "SpecificHeatCapacity", final unit = "J/(kg.K)");
      type n511 = Real(final quantity = "RatioOfSpecificHeatCapacities", final unit = "1");
      type n555 = Real(final quantity = "Entropy", final unit = "J/K");
      type n459 = Real(final quantity = "SpecificEntropy", final unit = "J/(kg.K)");
      type n457 = Real(final quantity = "SpecificEnergy", final unit = "J/kg");
      type n319 = n457;
      type n485 = Real(final unit = "s2/m2");
      type n488 = Real(final unit = "kg/(m3.K)");
      type n548 = Real(final quantity = "ElectricCharge", final unit = "C");
      type n556 = Real(final quantity = "AmountOfSubstance", final unit = "mol", min = 0);
      type n440 = Real(final quantity = "MolarMass", final unit = "kg/mol", min = 0);
      type n510 = Real(final quantity = "MolarVolume", final unit = "m3/mol", min = 0);
      type n320 = Real(final quantity = "MassFraction", final unit = "1", min = 0, max = 1);
      type n557 = Real(final quantity = "MoleFraction", final unit = "1", min = 0, max = 1);
      type n558 = Real(final quantity = "FaradayConstant", final unit = "C/mol");
      type n139 = Real(final quantity = "ReynoldsNumber", final unit = "1");
    end n11;

    package n443
      extends n1.n2.n3;
      type n444 = Real(final quantity = "ThermodynamicTemperature", final unit = "degC") annotation(absoluteValue = true);
      type n447 = Real(final quantity = "Pressure", final unit = "bar");
    end n443;

    package n18
      extends n1.n2.n3;

      function n445
        extends n1.n10.n2.n559;
        input n11.n57 n560;
        output n1.n10.n443.n444 n561;
      algorithm
        n561 := n560 + n1.n60.n551;
        annotation(Inline = true);
      end n445;

      function n428
        extends n1.n10.n2.n559;
        input n1.n10.n443.n444 n561;
        output n11.n57 n560;
      algorithm
        n560 := n561 - n1.n60.n551;
        annotation(Inline = true);
      end n428;

      function n448
        extends n1.n10.n2.n559;
        input n11.n216 n562;
        output n1.n10.n443.n447 n563;
      algorithm
        n563 := n562/1e5;
        annotation(Inline = true);
      end n448;
    end n18;

    package n2
      extends n1.n2.n543;

      partial function n559 end n559;
    end n2;
  end n10;
  annotation(version = "4.0.0", versionDate = "2020-06-04", dateModified = "2020-06-04 11:00:00Z");
end n1;

model n22_total
  extends n1.n16.n19.n21.n22;
 annotation(experiment(StopTime = 50));
end n22_total;
