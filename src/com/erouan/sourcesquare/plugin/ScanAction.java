package com.erouan.sourcesquare.plugin;

import com.antelink.sourcesquare.client.scan.ScanStatus;
import com.antelink.sourcesquare.client.scan.ScanStatusManager;
import com.antelink.sourcesquare.client.scan.SourceSquareEngine;
import com.antelink.sourcesquare.client.scan.SourceSquareFSWalker;
import com.antelink.sourcesquare.event.base.ClientEventHandler;
import com.antelink.sourcesquare.event.base.ClientEventType;
import com.antelink.sourcesquare.event.base.EventBus;
import com.antelink.sourcesquare.event.events.FilesIdentifiedEvent;
import com.antelink.sourcesquare.event.events.FilesScannedEvent;
import com.antelink.sourcesquare.event.events.OSFilesFoundEvent;
import com.antelink.sourcesquare.event.events.ScanCompleteEvent;
import com.antelink.sourcesquare.event.events.StartScanEvent;
import com.antelink.sourcesquare.event.handlers.FilesIdentifiedEventHandler;
import com.antelink.sourcesquare.event.handlers.FilesScannedEventHandler;
import com.antelink.sourcesquare.event.handlers.OSFilesFoundEventHandler;
import com.antelink.sourcesquare.event.handlers.ScanCompleteEventHandler;
import com.antelink.sourcesquare.query.AntepediaQuery;
import com.antelink.sourcesquare.results.ResultBuilder;
import com.antelink.sourcesquare.results.TreeMapBuilder;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.TaskInfo;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Antoine ROUAZE <antoine.rouaze AT zenika.com>
 */
public class ScanAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        String projectPath = project.getBasePath();
        startScan(projectPath, project);
    }

    private void startScan(String projectPath, final Project project) {
        final EventBus eventBus = new EventBus();
        AntepediaQuery query = new AntepediaQuery();
        SourceSquareEngine engine = new SourceSquareEngine(eventBus, query);
        ScanStatusManager manager = new ScanStatusManager(eventBus);
        manager.bind();
        TreeMapBuilder treemap = new TreeMapBuilder(eventBus);
        treemap.bind();
        ResultBuilder builder = new ResultBuilder(eventBus, treemap);
        builder.bind();
        final SourceSquareFSWalker walker = new SourceSquareFSWalker(engine, eventBus, treemap);
        walker.bind();
        final File file = new File(projectPath);

        Task task = new Task.Backgroundable(project, "Scan in progress") {
            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                eventBus.addHandler(ScanCompleteEvent.TYPE, new ScanCompleteEventHandler() {
                    @Override
                    public void handle(ArrayList<Integer> levels) {
                        System.out.println("Files scanned at the end: " + ScanStatus.INSTANCE.getNbFilesToScan());
                        ScanStatus.INSTANCE.setComplete();
                    }

                    @Override
                    public String getId() {
                        return getClass().getCanonicalName() + ": " + ScanCompleteEventHandler.class.getName();
                    }
                });
                eventBus.addHandler(FilesScannedEvent.TYPE, new FilesScannedEventHandler() {
                    @Override
                    public void handle(int count) {
                        ScanStatus.INSTANCE.setDisplayedFilesScanned(computeDisplayedFilesScanned());
                        ScanStatus status = ScanStatus.INSTANCE;
                        indicator.setText2("Files scanned: " + status.getDisplayedFilesScanned());
                    }

                    @Override
                    public String getId() {
                        return getClass().getCanonicalName() + ": " + FilesScannedEventHandler.class.getName();
                    }
                });
                eventBus.addHandler(FilesIdentifiedEvent.TYPE, new FilesIdentifiedEventHandler() {
                    @Override
                    public void handle(TreeSet<File> fileSet) {
                        indicator.setFraction(0);
                    }

                    @Override
                    public String getId() {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }
                });
                eventBus.fireEvent(new StartScanEvent(file));
                while (!ScanStatus.INSTANCE.isComplete()) {

                }
            }
        };
        ProgressManager.getInstance().run(task);

    }

    private int computeDisplayedFilesScanned() {
        long timeSinceStart = new Date().getTime() - ScanStatus.INSTANCE.getInitTime();
        long timeUntilLastScan = ScanStatus.INSTANCE.getLastUpdateTime()
                - ScanStatus.INSTANCE.getInitTime();
        if (timeUntilLastScan <= 0) {
            return ScanStatus.INSTANCE.getDisplayedFilesScanned();
        }
        int displayedfilesScanned = (int) (timeSinceStart * ScanStatus.INSTANCE.getNbFilesScanned() / timeUntilLastScan);
        if (displayedfilesScanned > ScanStatus.INSTANCE.getNbFilesScanned()
                + ScanStatus.INSTANCE.getNbQueryingFiles()
                || displayedfilesScanned > ScanStatus.INSTANCE.getNbFilesToScan()
                || displayedfilesScanned < ScanStatus.INSTANCE.getDisplayedFilesScanned()) {
            return ScanStatus.INSTANCE.getDisplayedFilesScanned();
        }
        return displayedfilesScanned;
    }

}
