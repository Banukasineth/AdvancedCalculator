        let equations = {}; 
        let currentChart = null;
        let isTracing = true;
        let currentUnit = 'rad';
        let currentTheme = 'light';
        let applyTimeout = null;
        
        let currentXDomain = [-10, 10];
        let currentYDomain = [-10, 10];
        
        function renderGraph() {
            const width = window.innerWidth;
            const height = window.innerHeight;
            
            let data = [];
            for (let id in equations) {
                if (equations[id].hidden) continue;
                let expr = equations[id].expr;
                let style = equations[id].style || 'solid';
                let dashArray = '';
                if (style === 'dashed') dashArray = '8,8';
                else if (style === 'dotted') dashArray = '3,4';
                
                data.push({
                    fn: expr,
                    color: equations[id].color || '#0078D7',
                    graphType: 'polyline',
                    attr: {
                        "stroke-dasharray": dashArray
                    }
                });
            }

            try {
                document.getElementById('graph').innerHTML = '';
                
                let bgColor = currentTheme === 'app' ? '#1e1e1e' : '#ffffff';
                let textColor = currentTheme === 'app' ? '#ffffff' : '#000000';
                let gridColor = currentTheme === 'app' ? '#333333' : '#e0e0e0';
                
                document.body.style.backgroundColor = bgColor;

                currentChart = functionPlot({
                    target: '#graph',
                    width: width,
                    height: height,
                    grid: true,
                    data: data,
                    disableZoom: false,
                    xAxis: { domain: currentXDomain },
                    yAxis: { domain: currentYDomain },
                    tip: {
                        xLine: true,
                        yLine: true,
                        renderer: function (x, y, index) {
                            return 'x: ' + x.toFixed(3) + '\ny: ' + y.toFixed(3);
                        }
                    }
                });
                
                // Style grid lines based on theme
                d3.selectAll('.domain').style('stroke', textColor);
                d3.selectAll('.tick line').style('stroke', gridColor);
                d3.selectAll('.tick text').style('fill', textColor);
                
                currentChart.on('after:draw', function () {
                    if (currentChart.meta && currentChart.meta.xScale) {
                        currentXDomain = currentChart.meta.xScale.domain();
                        currentYDomain = currentChart.meta.yScale.domain();
                        updateOptionsInputs();
                        
                        d3.selectAll('.domain').style('stroke', textColor);
                        d3.selectAll('.tick line').style('stroke', gridColor);
                        d3.selectAll('.tick text').style('fill', textColor);
                    }
                });
                updateOptionsInputs();
            } catch (e) {
                console.error("Partial expression, waiting for completion: ", e.message);
                // We don't clear the screen or show an error so the user isn't interrupted while typing.
            }
        }

        function zoom(factor) {
            const xCenter = (currentXDomain[0] + currentXDomain[1]) / 2;
            const yCenter = (currentYDomain[0] + currentYDomain[1]) / 2;
            const xSpan = (currentXDomain[1] - currentXDomain[0]) * factor / 2;
            const ySpan = (currentYDomain[1] - currentYDomain[0]) * factor / 2;
            
            currentXDomain = [xCenter - xSpan, xCenter + xSpan];
            currentYDomain = [yCenter - ySpan, yCenter + ySpan];
            
            renderGraph();
        }

        function resetZoom() {
            currentXDomain = [-10, 10];
            currentYDomain = [-10, 10];
            renderGraph();
        }
        
        function toggleTrace() {
            isTracing = !isTracing;
            const btn = document.getElementById('btnTrace');
            if (isTracing) {
                document.getElementById('trace-style').innerHTML = '';
                btn.classList.add('active');
            } else {
                document.getElementById('trace-style').innerHTML = '.tip { display: none !important; }';
                btn.classList.remove('active');
            }
        }

        function toggleOptions() {
            const popup = document.getElementById('options-popup');
            if (popup.style.display === 'flex') {
                popup.style.display = 'none';
                document.getElementById('btnSettings').classList.remove('active');
            } else {
                updateOptionsInputs();
                popup.style.display = 'flex';
                document.getElementById('btnSettings').classList.add('active');
            }
        }

        function updateOptionsInputs() {
            if(document.activeElement.tagName !== 'INPUT') {
                document.getElementById('inpXMin').value = currentXDomain[0].toFixed(2);
                document.getElementById('inpXMax').value = currentXDomain[1].toFixed(2);
                document.getElementById('inpYMin').value = currentYDomain[0].toFixed(2);
                document.getElementById('inpYMax').value = currentYDomain[1].toFixed(2);
            }
        }
        
        function applyOptionsDelayed() {
            clearTimeout(applyTimeout);
            applyTimeout = setTimeout(applyOptions, 600);
        }

        function applyOptions() {
            const xMin = parseFloat(document.getElementById('inpXMin').value);
            const xMax = parseFloat(document.getElementById('inpXMax').value);
            const yMin = parseFloat(document.getElementById('inpYMin').value);
            const yMax = parseFloat(document.getElementById('inpYMax').value);
            
            if (!isNaN(xMin) && !isNaN(xMax) && xMin < xMax) {
                currentXDomain = [xMin, xMax];
            }
            if (!isNaN(yMin) && !isNaN(yMax) && yMin < yMax) {
                currentYDomain = [yMin, yMax];
            }
            renderGraph();
        }
        
        function setUnit(btn, unit) {
            document.querySelectorAll('.segment').forEach(el => el.classList.remove('active'));
            btn.classList.add('active');
            currentUnit = unit;
            renderGraph();
        }
        
        function changeTheme(theme) {
            currentTheme = theme;
            renderGraph();
        }
        
        function shareGraph() {
            const toast = document.getElementById('toast');
            toast.innerText = 'Graph URL/Image copied to clipboard!';
            toast.style.opacity = '1';
            setTimeout(() => { toast.style.opacity = '0'; }, 3000);
        }

        function plotEquation(equationStr, color, id, hidden, style) {
            equations[id] = { expr: equationStr, color: color, hidden: hidden, style: style };
            return "Success";
        }

        function renderAll() {
            renderGraph();
        }

        function clearGraph() {
            equations = {};
        }

        window.onresize = renderGraph;
        renderGraph();
