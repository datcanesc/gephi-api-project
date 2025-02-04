package com.gephi.backend.service;

import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.appearance.api.Function;
import org.gephi.appearance.plugin.RankingElementColorTransformer;
import org.gephi.appearance.plugin.RankingNodeSizeTransformer;
// import org.gephi.filters.api.FilterController;
// import org.gephi.filters.api.Query;
// import org.gephi.filters.api.Range;
// import org.gephi.filters.plugin.graph.DegreeRangeBuilder.DegreeRangeFilter;
import org.gephi.graph.api.*;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.EdgeDirectionDefault;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.statistics.plugin.GraphDistance;
import org.openide.util.Lookup;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
import java.io.IOException;

@Service
public class GephiService {

    public File processGraphAndExportPdf(File inputFile, String outputFileName) {
        Workspace workspace = initProject();
        GraphModel graphModel = getGraphModel();
        PreviewModel previewModel = getPreviewModel();
        ImportController importController = getImportController();
        // FilterController filterController = getFilterController();
        AppearanceController appearanceController = getAppearanceController();
        AppearanceModel appearanceModel = appearanceController.getModel();

        Container container = importGraph(importController, inputFile);
        if (container == null) {
            throw new RuntimeException("Failed to import graph file.");
        }

        processGraphData(importController, container, workspace);

        DirectedGraph graph = graphModel.getDirectedGraph();
        printGraphStatistics(graph);

        // filterGraph(graphModel, filterController);
        runLayout(graphModel);
        computeGraphDistance(graphModel);
        applyNodeColorRanking(graphModel, appearanceController, appearanceModel);
        applyNodeSizeRanking(graphModel, appearanceController, appearanceModel);
        configurePreview(previewModel);

        return exportGraph(outputFileName);
    }

    private Workspace initProject() {
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        return pc.getCurrentWorkspace();
    }

    private GraphModel getGraphModel() {
        return Lookup.getDefault().lookup(GraphController.class).getGraphModel();
    }

    private PreviewModel getPreviewModel() {
        return Lookup.getDefault().lookup(PreviewController.class).getModel();
    }

    private ImportController getImportController() {
        return Lookup.getDefault().lookup(ImportController.class);
    }

    private AppearanceController getAppearanceController() {
        return Lookup.getDefault().lookup(AppearanceController.class);
    }

    private Container importGraph(ImportController importController, File inputFile) {
        try {
            Container container = importController.importFile(inputFile);
            container.getLoader().setEdgeDefault(EdgeDirectionDefault.DIRECTED);
            return container;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void processGraphData(ImportController importController, Container container, Workspace workspace) {
        importController.process(container, new DefaultProcessor(), workspace);
    }

    private void printGraphStatistics(DirectedGraph graph) {
        System.out.println("Nodes: " + graph.getNodeCount());
        System.out.println("Edges: " + graph.getEdgeCount());
    }

    // private FilterController getFilterController() {
    //     return Lookup.getDefault().lookup(FilterController.class);
    // }

    // private void filterGraph(GraphModel graphModel, FilterController filterController) {
    //     DegreeRangeFilter degreeFilter = new DegreeRangeFilter();
    //     degreeFilter.init(graphModel.getDirectedGraph());
    //     degreeFilter.setRange(new Range(30, Integer.MAX_VALUE));
    //     Query query = filterController.createQuery(degreeFilter);
    //     GraphView view = filterController.filter(query);
    //     graphModel.setVisibleView(view);

    //     UndirectedGraph visibleGraph = graphModel.getUndirectedGraphVisible();
    //     System.out.println("Visible Nodes: " + visibleGraph.getNodeCount());
    //     System.out.println("Visible Edges: " + visibleGraph.getEdgeCount());
    // }

    private void runLayout(GraphModel graphModel) {
        ForceAtlas2Builder layoutBuilder = new ForceAtlas2Builder();
        ForceAtlas2 layout = new ForceAtlas2(layoutBuilder);
        layout.setGraphModel(graphModel);
        layout.resetPropertiesValues();
        layout.initAlgo();

        for (int i = 0; i < 100 && layout.canAlgo(); i++) {
            layout.goAlgo();
        }
        layout.endAlgo();
    }

    private void computeGraphDistance(GraphModel graphModel) {
        GraphDistance distance = new GraphDistance();
        distance.setDirected(true);
        distance.execute(graphModel);
    }

    private void applyNodeColorRanking(GraphModel graphModel, AppearanceController appearanceController, AppearanceModel appearanceModel) {
        Function degreeRanking = appearanceModel.getNodeFunction(graphModel.defaultColumns().degree(), RankingElementColorTransformer.class);
        RankingElementColorTransformer degreeTransformer = (RankingElementColorTransformer) degreeRanking.getTransformer();
        degreeTransformer.setColors(new Color[]{
                new Color(0xFDDFAE),
                new Color(0x5E2AFA),
                new Color(0xB30000)
        });
        degreeTransformer.setColorPositions(new float[]{0f, 0.5f, 1f});
        appearanceController.transform(degreeRanking);
    }

    private void applyNodeSizeRanking(GraphModel graphModel, AppearanceController appearanceController, AppearanceModel appearanceModel) {
        Column centralityColumn = graphModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);
        Function centralityRanking = appearanceModel.getNodeFunction(centralityColumn, RankingNodeSizeTransformer.class);
        RankingNodeSizeTransformer centralityTransformer = (RankingNodeSizeTransformer) centralityRanking.getTransformer();
        centralityTransformer.setMinSize(5);
        centralityTransformer.setMaxSize(20);
        appearanceController.transform(centralityRanking);
    }

    private void configurePreview(PreviewModel previewModel) {
        previewModel.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(Color.GRAY));
        previewModel.getProperties().putValue(PreviewProperty.EDGE_THICKNESS, 0.1f);
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, previewModel.getProperties().getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(8f));
    }

    private File exportGraph(String outputFileName) {
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        File outputFile = new File(outputFileName);
        try {
            ec.exportFile(outputFile);
            return outputFile;
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to export graph to PDF: " + ex.getMessage());
        }
    }
}