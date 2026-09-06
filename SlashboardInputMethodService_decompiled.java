/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.content.ClipData
 *  android.content.ClipboardManager
 *  android.content.ClipboardManager$OnPrimaryClipChangedListener
 *  android.content.Context
 *  android.inputmethodservice.InputMethodService
 *  android.media.AudioAttributes
 *  android.media.AudioAttributes$Builder
 *  android.media.AudioManager
 *  android.media.SoundPool
 *  android.media.SoundPool$Builder
 *  android.os.Build$VERSION
 *  android.os.Handler
 *  android.os.Looper
 *  android.view.KeyEvent
 *  android.view.View
 *  android.view.Window
 *  android.view.inputmethod.EditorInfo
 *  android.view.inputmethod.ExtractedText
 *  android.view.inputmethod.ExtractedTextRequest
 *  android.view.inputmethod.InputConnection
 *  android.view.inputmethod.InputMethodManager
 *  android.widget.Toast
 *  androidx.compose.runtime.internal.StabilityInferred
 *  androidx.work.ExistingPeriodicWorkPolicy
 *  androidx.work.ExistingWorkPolicy
 *  androidx.work.OneTimeWorkRequest
 *  androidx.work.OneTimeWorkRequest$Builder
 *  androidx.work.PeriodicWorkRequest
 *  androidx.work.PeriodicWorkRequest$Builder
 *  androidx.work.WorkManager
 *  com.vanniktech.emoji.EmojiManager
 *  com.vanniktech.emoji.EmojiProvider
 *  com.vanniktech.emoji.ios.IosEmojiProvider
 *  kotlin.Metadata
 *  kotlin.Result
 *  kotlin.ResultKt
 *  kotlin.Unit
 *  kotlin.collections.CollectionsKt
 *  kotlin.collections.SetsKt
 *  kotlin.coroutines.Continuation
 *  kotlin.coroutines.CoroutineContext
 *  kotlin.coroutines.intrinsics.IntrinsicsKt
 *  kotlin.jvm.functions.Function0
 *  kotlin.jvm.functions.Function1
 *  kotlin.jvm.functions.Function2
 *  kotlin.jvm.internal.DefaultConstructorMarker
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.ranges.RangesKt
 *  kotlin.sequences.Sequence
 *  kotlin.sequences.SequencesKt
 *  kotlin.text.MatchResult
 *  kotlin.text.Regex
 *  kotlin.text.StringsKt
 *  kotlinx.coroutines.BuildersKt
 *  kotlinx.coroutines.CoroutineScope
 *  kotlinx.coroutines.CoroutineScopeKt
 *  kotlinx.coroutines.Dispatchers
 *  kotlinx.coroutines.SupervisorKt
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.slashboard.ime.R$raw
 */
package org.slashboard.ime.ime;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.compose.runtime.internal.StabilityInferred;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.EmojiProvider;
import com.vanniktech.emoji.ios.IosEmojiProvider;
import java.lang.invoke.LambdaMetafactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntPredicate;
import kotlin.Metadata;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.collections.SetsKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.RangesKt;
import kotlin.sequences.Sequence;
import kotlin.sequences.SequencesKt;
import kotlin.text.MatchResult;
import kotlin.text.Regex;
import kotlin.text.StringsKt;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.SupervisorKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slashboard.ime.CrashLogger;
import org.slashboard.ime.R;
import org.slashboard.ime.data.Candidate;
import org.slashboard.ime.data.ClipboardHistoryStore;
import org.slashboard.ime.data.EmojiRepository;
import org.slashboard.ime.data.LocalLearningStore;
import org.slashboard.ime.data.PredictionRepository;
import org.slashboard.ime.data.SlashboardSyncWorker;
import org.slashboard.ime.engine.CompositionSession;
import org.slashboard.ime.engine.FmConverter;
import org.slashboard.ime.engine.GraphemeDelete;
import org.slashboard.ime.engine.InputMode;
import org.slashboard.ime.engine.SinhalaEngine;
import org.slashboard.ime.engine.SlashboardEasterEgg;
import org.slashboard.ime.ime.EditorLayout;
import org.slashboard.ime.ime.KeyboardActions;
import org.slashboard.ime.ime.KeyboardView;
import org.slashboard.ime.ime.VoiceInputManager;
import org.slashboard.ime.settings.KeyboardPreferences;

@Metadata(mv={2, 0, 0}, k=1, xi=48, d1={"\u0000\u00ce\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010!\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u001a\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\u0007\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\b\b\u0007\u0018\u0000 }2\u00020\u00012\u00020\u0002:\u0001}B\u0007\u00a2\u0006\u0004\b\u0003\u0010\u0004J\b\u00107\u001a\u000208H\u0016J\b\u00109\u001a\u00020:H\u0016J\u001a\u0010;\u001a\u0002082\b\u0010<\u001a\u0004\u0018\u00010=2\u0006\u0010>\u001a\u00020(H\u0016J\b\u0010?\u001a\u000208H\u0016J\b\u0010@\u001a\u000208H\u0016J\u001a\u0010A\u001a\u0002082\b\u0010B\u001a\u0004\u0018\u00010=2\u0006\u0010>\u001a\u00020(H\u0016J\b\u0010C\u001a\u000208H\u0016J\b\u0010D\u001a\u000208H\u0016J\u0010\u0010E\u001a\u0002082\u0006\u0010F\u001a\u00020(H\u0016J8\u0010G\u001a\u0002082\u0006\u0010H\u001a\u00020\u000e2\u0006\u0010I\u001a\u00020\u000e2\u0006\u0010J\u001a\u00020\u000e2\u0006\u0010K\u001a\u00020\u000e2\u0006\u0010L\u001a\u00020\u000e2\u0006\u0010M\u001a\u00020\u000eH\u0016J\u0010\u0010N\u001a\u0002082\u0006\u0010O\u001a\u00020+H\u0016J\u0010\u0010P\u001a\u0002082\u0006\u0010Q\u001a\u00020(H\u0016J\b\u0010R\u001a\u000208H\u0016J\b\u0010S\u001a\u000208H\u0016J\u0010\u0010T\u001a\u0002082\u0006\u0010O\u001a\u00020+H\u0016J\b\u0010U\u001a\u000208H\u0016J\u0010\u0010V\u001a\u0002082\u0006\u0010W\u001a\u00020XH\u0016J\b\u0010Y\u001a\u000208H\u0016J\u0010\u0010Z\u001a\u0002082\u0006\u0010[\u001a\u00020+H\u0016J\b\u0010\\\u001a\u000208H\u0016J\u0010\u0010]\u001a\u0002082\u0006\u0010^\u001a\u00020\u000eH\u0016J\b\u0010_\u001a\u000208H\u0016J\u0010\u0010`\u001a\u00020a2\u0006\u0010b\u001a\u00020+H\u0016J\u0010\u0010c\u001a\u0002082\u0006\u0010d\u001a\u00020\u000eH\u0016J\b\u0010e\u001a\u000208H\u0016J\b\u0010f\u001a\u000208H\u0016J\u001a\u0010g\u001a\u00020(2\u0006\u0010h\u001a\u00020\u000e2\b\u0010i\u001a\u0004\u0018\u00010jH\u0016J\n\u0010k\u001a\u0004\u0018\u00010+H\u0002J\b\u0010l\u001a\u000208H\u0002J\u0010\u0010m\u001a\u0002082\u0006\u0010n\u001a\u00020(H\u0002J\u0010\u0010o\u001a\u0002082\u0006\u0010Q\u001a\u00020(H\u0002J\b\u0010p\u001a\u000208H\u0002J\u000e\u0010q\u001a\b\u0012\u0004\u0012\u00020+04H\u0002J\u0012\u0010r\u001a\u0002082\b\u0010Q\u001a\u0004\u0018\u00010+H\u0002J\b\u0010s\u001a\u000208H\u0002J\b\u0010t\u001a\u000208H\u0002J\b\u0010w\u001a\u000208H\u0002J\b\u0010x\u001a\u000208H\u0002J\u0010\u0010y\u001a\u0002082\u0006\u0010O\u001a\u00020+H\u0002J\b\u0010z\u001a\u000208H\u0002J\b\u0010{\u001a\u00020(H\u0002J\b\u0010|\u001a\u000208H\u0002R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000b\u001a\u0004\u0018\u00010\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0011\u001a\u0004\u0018\u00010\u0012X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0013\u001a\u0004\u0018\u00010\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0015\u001a\u0004\u0018\u00010\u0016X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0017\u001a\u0004\u0018\u00010\u0018X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0019\u001a\u00020\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0012\u0010\u001b\u001a\u00060\u001cj\u0002`\u001dX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0018\u0010\u001e\u001a\n  *\u0004\u0018\u00010\u001f0\u001fX\u0082\u0004\u00a2\u0006\u0004\n\u0002\u0010!R\u000e\u0010\"\u001a\u00020#X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010$\u001a\b\u0012\u0002\b\u0003\u0018\u00010%X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010&\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010'\u001a\u00020(X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010)\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010*\u001a\u0004\u0018\u00010+X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010,\u001a\b\u0012\u0004\u0012\u00020+0-X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010.\u001a\u00020/X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u00100\u001a\u0004\u0018\u000101X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00102\u001a\u00020(X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u00103\u001a\b\u0012\u0004\u0012\u00020+04X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00105\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00106\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010u\u001a\u00020vX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006~"}, d2={"Lorg/slashboard/ime/ime/SlashboardInputMethodService;", "Landroid/inputmethodservice/InputMethodService;", "Lorg/slashboard/ime/ime/KeyboardActions;", "<init>", "()V", "serviceScope", "Lkotlinx/coroutines/CoroutineScope;", "prefs", "Lorg/slashboard/ime/settings/KeyboardPreferences;", "keyboard", "Lorg/slashboard/ime/ime/KeyboardView;", "soundPool", "Landroid/media/SoundPool;", "soundIos", "", "soundMech", "soundType", "learning", "Lorg/slashboard/ime/data/LocalLearningStore;", "prediction", "Lorg/slashboard/ime/data/PredictionRepository;", "emoji", "Lorg/slashboard/ime/data/EmojiRepository;", "clipboardHistory", "Lorg/slashboard/ime/data/ClipboardHistoryStore;", "composition", "Lorg/slashboard/ime/engine/CompositionSession;", "slsSource", "Ljava/lang/StringBuilder;", "Lkotlin/text/StringBuilder;", "executor", "Ljava/util/concurrent/ExecutorService;", "kotlin.jvm.PlatformType", "Ljava/util/concurrent/ExecutorService;", "main", "Landroid/os/Handler;", "predictionTask", "Ljava/util/concurrent/Future;", "generation", "restricted", "", "lastSelectionEnd", "previousCommittedWord", "", "recentEmoji", "", "editorLayout", "Lorg/slashboard/ime/ime/EditorLayout;", "voiceInputManager", "Lorg/slashboard/ime/ime/VoiceInputManager;", "precedingDirty", "cachedPreceding", "", "deleteAnchor", "deleteLength", "onCreate", "", "onCreateInputView", "Landroid/view/View;", "onStartInput", "attribute", "Landroid/view/inputmethod/EditorInfo;", "restarting", "onBindInput", "onUnbindInput", "onStartInputView", "info", "onFinishInput", "onDestroy", "onFinishInputView", "finishingInput", "onUpdateSelection", "oldSelStart", "oldSelEnd", "newSelStart", "newSelEnd", "candidatesStart", "candidatesEnd", "onCharacter", "value", "onBackspace", "word", "onSpace", "onEnter", "onCandidate", "onGlobe", "onModeRequested", "mode", "Lorg/slashboard/ime/engine/InputMode;", "onHide", "onToolbarAction", "action", "onVoiceInputRequested", "onCursorDelta", "delta", "onPressFeedback", "languageScoreForKey", "", "output", "onPreviewDelete", "clusters", "onCommitPreviewDelete", "onCancelPreviewDelete", "onKeyDown", "keyCode", "event", "Landroid/view/KeyEvent;", "commitComposition", "clearLocalCompositionState", "cancelComposition", "removeHostText", "deleteFromHost", "updateSuggestions", "precedingWords", "learn", "captureClipboard", "checkOtp", "clipListener", "Landroid/content/ClipboardManager$OnPrimaryClipChangedListener;", "listenForClipboard", "stopClipboardListener", "rememberEmoji", "feedback", "offerSystemSwitch", "switchSystemKeyboard", "Companion", "app_debug"})
@StabilityInferred(parameters=0)
@SourceDebugExtension(value={"SMAP\nSlashboardInputMethodService.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SlashboardInputMethodService.kt\norg/slashboard/ime/ime/SlashboardInputMethodService\n+ 2 PeriodicWorkRequest.kt\nandroidx/work/PeriodicWorkRequestKt\n+ 3 OneTimeWorkRequest.kt\nandroidx/work/OneTimeWorkRequestKt\n+ 4 fake.kt\nkotlin/jvm/internal/FakeKt\n+ 5 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,598:1\n368#2:599\n105#3:600\n1#4:601\n1557#5:602\n1628#5,3:603\n*S KotlinDebug\n*F\n+ 1 SlashboardInputMethodService.kt\norg/slashboard/ime/ime/SlashboardInputMethodService\n*L\n107#1:599\n116#1:600\n470#1:602\n470#1:603,3\n*E\n"})
public final class SlashboardInputMethodService
extends InputMethodService
implements KeyboardActions {
    @NotNull
    public static final Companion Companion = new Companion(null);
    @NotNull
    private final CoroutineScope serviceScope = CoroutineScopeKt.CoroutineScope((CoroutineContext)SupervisorKt.SupervisorJob$default(null, (int)1, null).plus((CoroutineContext)Dispatchers.getMain().getImmediate()));
    private KeyboardPreferences prefs;
    private KeyboardView keyboard;
    @Nullable
    private SoundPool soundPool;
    private int soundIos;
    private int soundMech;
    private int soundType;
    @Nullable
    private LocalLearningStore learning;
    @Nullable
    private PredictionRepository prediction;
    @Nullable
    private EmojiRepository emoji;
    @Nullable
    private ClipboardHistoryStore clipboardHistory;
    @NotNull
    private final CompositionSession composition = new CompositionSession();
    @NotNull
    private final StringBuilder slsSource = new StringBuilder();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    @NotNull
    private final Handler main = new Handler(Looper.getMainLooper());
    @Nullable
    private Future<?> predictionTask;
    private int generation;
    private boolean restricted;
    private int lastSelectionEnd = -1;
    @Nullable
    private String previousCommittedWord;
    @NotNull
    private List<String> recentEmoji = new ArrayList();
    @NotNull
    private EditorLayout editorLayout = EditorLayout.TEXT;
    @Nullable
    private VoiceInputManager voiceInputManager;
    private boolean precedingDirty = true;
    @NotNull
    private List<String> cachedPreceding = CollectionsKt.emptyList();
    private int deleteAnchor = -1;
    private int deleteLength;
    @NotNull
    private final ClipboardManager.OnPrimaryClipChangedListener clipListener = () -> SlashboardInputMethodService.clipListener$lambda$38(this);
    public static final int $stable = 8;

    /*
     * WARNING - void declaration
     */
    public void onCreate() {
        Object $this$onCreate_u24lambda_u2412;
        super.onCreate();
        CrashLogger.INSTANCE.init((Context)this);
        AudioAttributes attrs = new AudioAttributes.Builder().setUsage(13).setContentType(4).build();
        SoundPool soundPool = this.soundPool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build();
        if (soundPool != null) {
            SoundPool it = soundPool;
            boolean bl = false;
            this.soundIos = it.load((Context)this, R.raw.sound_ios, 1);
            this.soundMech = it.load((Context)this, R.raw.sound_mechanical, 1);
            this.soundType = it.load((Context)this, R.raw.sound_typewriter, 1);
        }
        SlashboardInputMethodService slashboardInputMethodService = this;
        try {
            $this$onCreate_u24lambda_u2412 = slashboardInputMethodService;
            boolean bl = false;
            EmojiManager.install((EmojiProvider)((EmojiProvider)new IosEmojiProvider()));
            $this$onCreate_u24lambda_u2412 = Result.constructor-impl((Object)Unit.INSTANCE);
        }
        catch (Throwable bl) {
            $this$onCreate_u24lambda_u2412 = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)bl));
        }
        this.prefs = new KeyboardPreferences((Context)this);
        this.voiceInputManager = new VoiceInputManager((Context)this, (Function1<? super String, Unit>)((Function1)arg_0 -> SlashboardInputMethodService.onCreate$lambda$2(this, arg_0)), (Function1<? super String, Unit>)((Function1)arg_0 -> SlashboardInputMethodService.onCreate$lambda$3(this, arg_0)), (Function1<? super Integer, Unit>)((Function1)arg_0 -> SlashboardInputMethodService.onCreate$lambda$4(this, arg_0)), (Function0<Unit>)((Function0)() -> SlashboardInputMethodService.onCreate$lambda$5(this)));
        try {
            void repeatIntervalTimeUnit$iv;
            void repeatInterval$iv;
            long $this$onCreate_u24lambda_u2412 = 1L;
            TimeUnit bl = TimeUnit.DAYS;
            boolean $i$f$PeriodicWorkRequestBuilder = false;
            PeriodicWorkRequest workRequest = (PeriodicWorkRequest)new PeriodicWorkRequest.Builder(SlashboardSyncWorker.class, (long)repeatInterval$iv, (TimeUnit)repeatIntervalTimeUnit$iv).build();
            WorkManager.Companion.getInstance((Context)this).enqueueUniquePeriodicWork("SlashboardBackgroundSync", ExistingPeriodicWorkPolicy.KEEP, workRequest);
            boolean $i$f$OneTimeWorkRequestBuilder = false;
            OneTimeWorkRequest initialRequest = (OneTimeWorkRequest)new OneTimeWorkRequest.Builder(SlashboardSyncWorker.class).build();
            WorkManager.Companion.getInstance((Context)this).enqueueUniqueWork("SlashboardInitialSync", ExistingWorkPolicy.KEEP, initialRequest);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        BuildersKt.launch$default((CoroutineScope)this.serviceScope, (CoroutineContext)((CoroutineContext)Dispatchers.getIO()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            int label;
            final /* synthetic */ SlashboardInputMethodService this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * WARNING - void declaration
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object object) {
                Object object2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)object);
                        LocalLearningStore localLearning = new LocalLearningStore((Context)this.this$0);
                        PredictionRepository predictionRepo = new PredictionRepository((Context)this.this$0, localLearning);
                        EmojiRepository emojiRepo = new EmojiRepository((Context)this.this$0);
                        ClipboardHistoryStore clipboardStore = new ClipboardHistoryStore((Context)this.this$0);
                        predictionRepo.warmup();
                        SlashboardInputMethodService.access$setLearning$p(this.this$0, localLearning);
                        SlashboardInputMethodService.access$setPrediction$p(this.this$0, predictionRepo);
                        SlashboardInputMethodService.access$setEmoji$p(this.this$0, emojiRepo);
                        SlashboardInputMethodService.access$setClipboardHistory$p(this.this$0, clipboardStore);
                        this.label = 1;
                        Object object3 = BuildersKt.withContext((CoroutineContext)((CoroutineContext)Dispatchers.getMain()), (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this.this$0, emojiRepo, clipboardStore, null){
                            int label;
                            final /* synthetic */ SlashboardInputMethodService this$0;
                            final /* synthetic */ EmojiRepository $emojiRepo;
                            final /* synthetic */ ClipboardHistoryStore $clipboardStore;
                            {
                                this.this$0 = $receiver;
                                this.$emojiRepo = $emojiRepo;
                                this.$clipboardStore = $clipboardStore;
                                super(2, $completion);
                            }

                            public final Object invokeSuspend(Object object) {
                                IntrinsicsKt.getCOROUTINE_SUSPENDED();
                                switch (this.label) {
                                    case 0: {
                                        ResultKt.throwOnFailure((Object)object);
                                        if (SlashboardInputMethodService.access$getKeyboard$p(this.this$0) != null) {
                                            KeyboardView keyboardView = SlashboardInputMethodService.access$getKeyboard$p(this.this$0);
                                            if (keyboardView == null) {
                                                Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
                                                keyboardView = null;
                                            }
                                            keyboardView.updateRepositories(this.$emojiRepo, this.$clipboardStore);
                                            KeyboardView keyboardView2 = SlashboardInputMethodService.access$getKeyboard$p(this.this$0);
                                            if (keyboardView2 == null) {
                                                Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
                                                keyboardView2 = null;
                                            }
                                            keyboardView2.setClipboardItems(this.$clipboardStore.items(), this.$clipboardStore.pinnedItems());
                                            SlashboardInputMethodService.access$updateSuggestions(this.this$0);
                                        }
                                        return Unit.INSTANCE;
                                    }
                                }
                                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                            }

                            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                                return (Continuation)new /* invalid duplicate definition of identical inner class */;
                            }

                            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                            }
                        }), (Continuation)((Continuation)this));
                        if (object3 != object2) return Unit.INSTANCE;
                        return object2;
                    }
                    case 1: {
                        void $result;
                        ResultKt.throwOnFailure((Object)$result);
                        Object object3 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)2, null);
    }

    @NotNull
    public View onCreateInputView() {
        KeyboardView keyboardView;
        Context context = (Context)this;
        KeyboardActions keyboardActions = this;
        KeyboardPreferences keyboardPreferences = this.prefs;
        if (keyboardPreferences == null) {
            Intrinsics.throwUninitializedPropertyAccessException((String)"prefs");
            keyboardPreferences = null;
        }
        if ((keyboardView = (this.keyboard = new KeyboardView(context, keyboardActions, keyboardPreferences, this.emoji, this.clipboardHistory))) == null) {
            Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
            keyboardView = null;
        }
        return (View)keyboardView;
    }

    /*
     * WARNING - void declaration
     */
    public void onStartInput(@Nullable EditorInfo attribute, boolean restarting) {
        boolean bl;
        super.onStartInput(attribute, restarting);
        if (!restarting) {
            this.cancelComposition(false);
        }
        SlashboardInputMethodService slashboardInputMethodService = this;
        EditorInfo editorInfo = attribute;
        if (editorInfo != null) {
            void p0;
            EditorInfo editorInfo2 = editorInfo;
            Companion companion = Companion;
            EditorInfo editorInfo3 = editorInfo2;
            SlashboardInputMethodService slashboardInputMethodService2 = slashboardInputMethodService;
            boolean $i$f$onStartInput$stub_for_inlining = false;
            boolean bl2 = false;
            boolean bl3 = companion.isRestrictedEditor((EditorInfo)p0);
            slashboardInputMethodService = slashboardInputMethodService2;
            bl = bl3;
        } else {
            bl = true;
        }
        slashboardInputMethodService.restricted = bl;
        EditorInfo editorInfo4 = attribute;
        this.lastSelectionEnd = editorInfo4 != null ? editorInfo4.initialSelEnd : -1;
        this.precedingDirty = true;
    }

    public void onBindInput() {
        super.onBindInput();
        this.clearLocalCompositionState();
        this.precedingDirty = true;
    }

    public void onUnbindInput() {
        super.onUnbindInput();
        this.clearLocalCompositionState();
        this.precedingDirty = true;
    }

    /*
     * WARNING - void declaration
     */
    public void onStartInputView(@Nullable EditorInfo info, boolean restarting) {
        KeyboardPreferences keyboardPreferences;
        boolean bl;
        super.onStartInputView(info, restarting);
        this.prefs = new KeyboardPreferences((Context)this);
        SlashboardInputMethodService slashboardInputMethodService = this;
        EditorInfo editorInfo = info;
        if (editorInfo != null) {
            void p0;
            EditorInfo editorInfo2 = editorInfo;
            Companion companion = Companion;
            EditorInfo editorInfo3 = editorInfo2;
            SlashboardInputMethodService slashboardInputMethodService2 = slashboardInputMethodService;
            boolean $i$f$onStartInputView$stub_for_inlining$6 = false;
            boolean bl2 = false;
            boolean bl3 = companion.isRestrictedEditor((EditorInfo)p0);
            slashboardInputMethodService = slashboardInputMethodService2;
            bl = bl3;
        } else {
            bl = true;
        }
        slashboardInputMethodService.restricted = bl;
        this.editorLayout = Companion.editorLayout(info);
        KeyboardView keyboardView = this.keyboard;
        if (keyboardView == null) {
            Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
            keyboardView = null;
        }
        if ((keyboardPreferences = this.prefs) == null) {
            Intrinsics.throwUninitializedPropertyAccessException((String)"prefs");
            keyboardPreferences = null;
        }
        keyboardView.configure(keyboardPreferences.getMode(), this.offerSystemSwitch(), Companion.enterLabel(info), this.editorLayout);
        KeyboardView keyboardView2 = this.keyboard;
        if (keyboardView2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
            keyboardView2 = null;
        }
        keyboardView2.setLearningEnabled(!this.restricted && this.editorLayout == EditorLayout.TEXT);
        this.checkOtp();
        KeyboardPreferences keyboardPreferences2 = this.prefs;
        if (keyboardPreferences2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException((String)"prefs");
            keyboardPreferences2 = null;
        }
        if (keyboardPreferences2.getClipboardHistory()) {
            this.captureClipboard();
        }
        ClipboardHistoryStore clipboardHistoryStore = this.clipboardHistory;
        if (clipboardHistoryStore != null) {
            ClipboardHistoryStore it = clipboardHistoryStore;
            boolean bl4 = false;
            KeyboardView keyboardView3 = this.keyboard;
            if (keyboardView3 == null) {
                Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
                keyboardView3 = null;
            }
            keyboardView3.setClipboardItems(it.items(), it.pinnedItems());
        }
        this.listenForClipboard();
        KeyboardView keyboardView4 = this.keyboard;
        if (keyboardView4 == null) {
            Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
            keyboardView4 = null;
        }
        keyboardView4.setRecentEmoji(this.recentEmoji);
        this.updateSuggestions();
    }

    public void onFinishInput() {
        this.deleteAnchor = -1;
        this.deleteLength = 0;
        this.cancelComposition(false);
        super.onFinishInput();
    }

    public void onDestroy() {
        VoiceInputManager voiceInputManager = this.voiceInputManager;
        if (voiceInputManager != null) {
            voiceInputManager.destroy();
        }
        SoundPool soundPool = this.soundPool;
        if (soundPool != null) {
            soundPool.release();
        }
        this.stopClipboardListener();
        CoroutineScopeKt.cancel$default((CoroutineScope)this.serviceScope, null, (int)1, null);
        this.executor.shutdown();
        super.onDestroy();
    }

    public void onFinishInputView(boolean finishingInput) {
        this.stopClipboardListener();
        this.cancelComposition(false);
        super.onFinishInputView(finishingInput);
    }

    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        if (this.composition.getActive() && candidatesStart >= 0 && (newSelEnd < candidatesStart || newSelEnd > candidatesEnd)) {
            this.cancelComposition(false);
        }
        this.lastSelectionEnd = newSelEnd;
    }

    /*
     * Unable to fully structure code
     */
    @Override
    public void onCharacter(@NotNull String value) {
        Intrinsics.checkNotNullParameter((Object)value, (String)"value");
        var2_2 = this;
        try {
            $this$onCharacter_u24lambda_u249 = var2_2;
            $i$a$-runCatching-SlashboardInputMethodService$onCharacter$1 = false;
            if ($this$onCharacter_u24lambda_u249.editorLayout != EditorLayout.TEXT) ** GOTO lbl-1000
            v0 = $this$onCharacter_u24lambda_u249.prefs;
            if (v0 == null) {
                Intrinsics.throwUninitializedPropertyAccessException((String)"prefs");
                v0 = null;
            }
            if (v0.getUseEnglish()) lbl-1000:
            // 2 sources

            {
                $this$onCharacter_u24lambda_u249.commitComposition();
                v1 = $this$onCharacter_u24lambda_u249.getCurrentInputConnection();
                if (v1 != null) {
                    v1.commitText((CharSequence)value, 1);
                }
            } else if (value.length() == 1 && Character.isLetter(value.charAt(0)) && value.charAt(0) < '\u0080') {
                v2 = $this$onCharacter_u24lambda_u249.composition;
                v3 = $this$onCharacter_u24lambda_u249.prefs;
                if (v3 == null) {
                    Intrinsics.throwUninitializedPropertyAccessException((String)"prefs");
                    v3 = null;
                }
                rendered = v2.type(value, v3.getMode());
                v4 = $this$onCharacter_u24lambda_u249.getCurrentInputConnection();
                if (v4 != null) {
                    v4.setComposingText((CharSequence)rendered, 1);
                }
            } else {
                $this$onCharacter_u24lambda_u249.commitComposition();
                v5 = $this$onCharacter_u24lambda_u249.getCurrentInputConnection();
                if (v5 != null) {
                    v5.commitText((CharSequence)value, 1);
                }
                if (value.codePoints().anyMatch((IntPredicate)LambdaMetafactory.metafactory(null, null, null, (I)Z, onCharacter$lambda$9$lambda$8(int ), (I)Z)())) {
                    $this$onCharacter_u24lambda_u249.rememberEmoji(value);
                }
            }
            $this$onCharacter_u24lambda_u249.updateSuggestions();
            var3_3 = Result.constructor-impl((Object)Unit.INSTANCE);
        }
        catch (Throwable var4_6) {
            var3_4 = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)var4_6));
        }
    }

    @Override
    public void onBackspace(boolean word) {
        SlashboardInputMethodService slashboardInputMethodService = this;
        try {
            SlashboardInputMethodService $this$onBackspace_u24lambda_u2410 = slashboardInputMethodService;
            boolean bl = false;
            if ($this$onBackspace_u24lambda_u2410.composition.getActive()) {
                String rendered;
                CompositionSession compositionSession = $this$onBackspace_u24lambda_u2410.composition;
                KeyboardPreferences keyboardPreferences = $this$onBackspace_u24lambda_u2410.prefs;
                if (keyboardPreferences == null) {
                    Intrinsics.throwUninitializedPropertyAccessException((String)"prefs");
                    keyboardPreferences = null;
                }
                if (((CharSequence)(rendered = compositionSession.backspace(keyboardPreferences.getMode()))).length() == 0) {
                    InputConnection inputConnection = $this$onBackspace_u24lambda_u2410.getCurrentInputConnection();
                    if (inputConnection != null) {
                        inputConnection.setComposingText((CharSequence)"", 1);
                    }
                    InputConnection inputConnection2 = $this$onBackspace_u24lambda_u2410.getCurrentInputConnection();
                    if (inputConnection2 != null) {
                        inputConnection2.finishComposingText();
                    }
                } else {
                    InputConnection inputConnection = $this$onBackspace_u24lambda_u2410.getCurrentInputConnection();
                    if (inputConnection != null) {
                        inputConnection.setComposingText((CharSequence)rendered, 1);
                    }
                }
            } else {
                $this$onBackspace_u24lambda_u2410.deleteFromHost(word);
                $this$onBackspace_u24lambda_u2410.precedingDirty = true;
            }
            $this$onBackspace_u24lambda_u2410.updateSuggestions();
            Object object = Result.constructor-impl((Object)Unit.INSTANCE);
        }
        catch (Throwable throwable) {
            Object object = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)throwable));
        }
    }

    @Override
    public void onSpace() {
        SlashboardInputMethodService slashboardInputMethodService = this;
        try {
            SlashboardInputMethodService $this$onSpace_u24lambda_u2411 = slashboardInputMethodService;
            boolean bl = false;
            String word = $this$onSpace_u24lambda_u2411.commitComposition();
            InputConnection inputConnection = $this$onSpace_u24lambda_u2411.getCurrentInputConnection();
            if (inputConnection != null) {
                inputConnection.commitText((CharSequence)" ", 1);
            }
            $this$onSpace_u24lambda_u2411.learn(word);
            $this$onSpace_u24lambda_u2411.precedingDirty = true;
            $this$onSpace_u24lambda_u2411.updateSuggestions();
            Object object = Result.constructor-impl((Object)Unit.INSTANCE);
        }
        catch (Throwable throwable) {
            Object object = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)throwable));
        }
    }

    @Override
    public void onEnter() {
        SlashboardInputMethodService slashboardInputMethodService = this;
        try {
            int action;
            EditorInfo info;
            SlashboardInputMethodService $this$onEnter_u24lambda_u2413 = slashboardInputMethodService;
            boolean bl = false;
            String word = $this$onEnter_u24lambda_u2413.commitComposition();
            $this$onEnter_u24lambda_u2413.learn(word);
            EditorInfo editorInfo = info = $this$onEnter_u24lambda_u2413.getCurrentInputEditorInfo();
            int n = action = editorInfo != null ? editorInfo.imeOptions & 0xFF : 1;
            if (action != 1 && action != 0) {
                InputConnection inputConnection = $this$onEnter_u24lambda_u2413.getCurrentInputConnection();
                if (inputConnection != null) {
                    inputConnection.performEditorAction(action);
                }
                if (action == 6) {
                    $this$onEnter_u24lambda_u2413.requestHideSelf(0);
                }
            } else {
                Boolean bl2;
                InputConnection inputConnection = $this$onEnter_u24lambda_u2413.getCurrentInputConnection();
                Boolean it = bl2 = inputConnection != null ? Boolean.valueOf(inputConnection.sendKeyEvent(new KeyEvent(0, 66))) : null;
                boolean bl3 = false;
                InputConnection inputConnection2 = $this$onEnter_u24lambda_u2413.getCurrentInputConnection();
                if (inputConnection2 != null) {
                    inputConnection2.sendKeyEvent(new KeyEvent(1, 66));
                }
            }
            $this$onEnter_u24lambda_u2413.cancelComposition(false);
            $this$onEnter_u24lambda_u2413.updateSuggestions();
            Object object = Result.constructor-impl((Object)Unit.INSTANCE);
        }
        catch (Throwable throwable) {
            Object object = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)throwable));
        }
    }

    @Override
    public void onCandidate(@NotNull String value) {
        Intrinsics.checkNotNullParameter((Object)value, (String)"value");
        SlashboardInputMethodService slashboardInputMethodService = this;
        try {
            SlashboardInputMethodService $this$onCandidate_u24lambda_u2414 = slashboardInputMethodService;
            boolean bl = false;
            $this$onCandidate_u24lambda_u2414.feedback();
            if (Intrinsics.areEqual((Object)value, (Object)"\u2726 \u0d85\u0d9a\u0dca\u0dc2\u0dbb")) {
                InputConnection inputConnection = $this$onCandidate_u24lambda_u2414.getCurrentInputConnection();
                if (inputConnection != null) {
                    inputConnection.setComposingText((CharSequence)"Made in Sri Lanka \ud83c\uddf1\ud83c\uddf0", 1);
                }
                InputConnection inputConnection2 = $this$onCandidate_u24lambda_u2414.getCurrentInputConnection();
                if (inputConnection2 != null) {
                    inputConnection2.finishComposingText();
                }
                $this$onCandidate_u24lambda_u2414.composition.clear();
                StringsKt.clear((StringBuilder)$this$onCandidate_u24lambda_u2414.slsSource);
                $this$onCandidate_u24lambda_u2414.updateSuggestions();
                return;
            }
            InputConnection inputConnection = $this$onCandidate_u24lambda_u2414.getCurrentInputConnection();
            if (inputConnection != null) {
                inputConnection.setComposingText((CharSequence)value, 1);
            }
            InputConnection inputConnection3 = $this$onCandidate_u24lambda_u2414.getCurrentInputConnection();
            if (inputConnection3 != null) {
                inputConnection3.finishComposingText();
            }
            $this$onCandidate_u24lambda_u2414.composition.clear();
            StringsKt.clear((StringBuilder)$this$onCandidate_u24lambda_u2414.slsSource);
            $this$onCandidate_u24lambda_u2414.learn(value);
            InputConnection inputConnection4 = $this$onCandidate_u24lambda_u2414.getCurrentInputConnection();
            if (inputConnection4 != null) {
                inputConnection4.commitText((CharSequence)" ", 1);
            }
            $this$onCandidate_u24lambda_u2414.precedingDirty = true;
            $this$onCandidate_u24lambda_u2414.updateSuggestions();
            Object object = Result.constructor-impl((Object)Unit.INSTANCE);
        }
        catch (Throwable throwable) {
            Object object = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)throwable));
        }
    }

    @Override
    public void onGlobe() {
        SlashboardInputMethodService slashboardInputMethodService = this;
        try {
            SlashboardInputMethodService $this$onGlobe_u24lambda_u2415 = slashboardInputMethodService;
            boolean bl = false;
            $this$onGlobe_u24lambda_u2415.commitComposition();
            $this$onGlobe_u24lambda_u2415.switchSystemKeyboard();
            Object object = Result.constructor-impl((Object)Unit.INSTANCE);
        }
        catch (Throwable throwable) {
            Object object = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)throwable));
        }
    }

    @Override
    public void onModeRequested(@NotNull InputMode mode) {
        Intrinsics.checkNotNullParameter((Object)((Object)mode), (String)"mode");
        SlashboardInputMethodService slashboardInputMethodService = this;
        try {
            SlashboardInputMethodService $this$onModeRequested_u24lambda_u2416 = slashboardInputMethodService;
            boolean bl = false;
            $this$onModeRequested_u24lambda_u2416.commitComposition();
            KeyboardPreferences keyboardPreferences = $this$onModeRequested_u24lambda_u2416.prefs;
            if (keyboardPreferences == null) {
                Intrinsics.throwUninitializedPropertyAccessException((String)"prefs");
                keyboardPreferences = null;
            }
            keyboardPreferences.setMode(mode);
            KeyboardView keyboardView = $this$onModeRequested_u24lambda_u2416.keyboard;
            if (keyboardView == null) {
                Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
                keyboardView = null;
            }
            keyboardView.configure(mode, $this$onModeRequested_u24lambda_u2416.offerSystemSwitch(), Companion.enterLabel($this$onModeRequested_u24lambda_u2416.getCurrentInputEditorInfo()), $this$onModeRequested_u24lambda_u2416.editorLayout);
            Object object = Result.constructor-impl((Object)Unit.INSTANCE);
        }
        catch (Throwable throwable) {
            Object object = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)throwable));
        }
    }

    @Override
    public void onHide() {
        SlashboardInputMethodService slashboardInputMethodService = this;
        try {
            SlashboardInputMethodService $this$onHide_u24lambda_u2417 = slashboardInputMethodService;
            boolean bl = false;
            $this$onHide_u24lambda_u2417.commitComposition();
            $this$onHide_u24lambda_u2417.requestHideSelf(0);
            Object object = Result.constructor-impl((Object)Unit.INSTANCE);
        }
        catch (Throwable throwable) {
            Object object = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)throwable));
        }
    }

    @Override
    public void onToolbarAction(@NotNull String action) {
        Intrinsics.checkNotNullParameter((Object)action, (String)"action");
        InputConnection inputConnection = this.getCurrentInputConnection();
        if (inputConnection == null) {
            return;
        }
        InputConnection ic = inputConnection;
        switch (action) {
            case "fm": {
                ExtractedText extractedText = ic.getExtractedText(new ExtractedTextRequest(), 0);
                if (extractedText == null) {
                    return;
                }
                ExtractedText extracted = extractedText;
                CharSequence charSequence = extracted.text;
                if (charSequence == null || (charSequence = ((Object)charSequence).toString()) == null) {
                    return;
                }
                CharSequence text = charSequence;
                if (!(text.length() > 0)) break;
                String converted = FmConverter.INSTANCE.convert((String)text);
                ic.setSelection(0, ((String)text).length());
                ic.commitText((CharSequence)converted, 1);
                break;
            }
            case "translate": {
                ExtractedText extractedText = ic.getExtractedText(new ExtractedTextRequest(), 0);
                if (extractedText == null) {
                    return;
                }
                ExtractedText extracted = extractedText;
                CharSequence charSequence = extracted.text;
                if (charSequence == null || (charSequence = ((Object)charSequence).toString()) == null) {
                    return;
                }
                CharSequence text = charSequence;
                if (!(text.length() > 0)) break;
                ic.commitText((CharSequence)" [Translated] ", 1);
                break;
            }
            case "otp": {
                String otp;
                CharSequence charSequence;
                String text;
                Object object = this.getSystemService("clipboard");
                Intrinsics.checkNotNull((Object)object, (String)"null cannot be cast to non-null type android.content.ClipboardManager");
                ClipboardManager cm = (ClipboardManager)object;
                ClipData clip = cm.getPrimaryClip();
                if (clip == null || clip.getItemCount() <= 0) break;
                CharSequence charSequence2 = clip.getItemAt(0).getText();
                String string = text = charSequence2 != null ? ((Object)charSequence2).toString() : null;
                if (text == null || !new Regex(".*\\b\\d{4,8}\\b.*").matches(charSequence = (CharSequence)text)) break;
                MatchResult matchResult = Regex.find$default((Regex)new Regex("\\b\\d{4,8}\\b"), (CharSequence)text, (int)0, (int)2, null);
                String string2 = otp = matchResult != null ? matchResult.getValue() : null;
                if (otp == null) break;
                ic.commitText((CharSequence)otp, 1);
            }
        }
    }

    @Override
    public void onVoiceInputRequested() {
        block1: {
            VoiceInputManager voiceInputManager = this.voiceInputManager;
            if (voiceInputManager == null) break block1;
            KeyboardPreferences keyboardPreferences = this.prefs;
            if (keyboardPreferences == null) {
                Intrinsics.throwUninitializedPropertyAccessException((String)"prefs");
                keyboardPreferences = null;
            }
            voiceInputManager.startListening(keyboardPreferences.getUseEnglish());
        }
    }

    @Override
    public void onCursorDelta(int delta) {
        SlashboardInputMethodService slashboardInputMethodService = this;
        try {
            SlashboardInputMethodService $this$onCursorDelta_u24lambda_u2418 = slashboardInputMethodService;
            boolean bl = false;
            if (delta == 0) {
                return;
            }
            $this$onCursorDelta_u24lambda_u2418.commitComposition();
            InputConnection inputConnection = $this$onCursorDelta_u24lambda_u2418.getCurrentInputConnection();
            if (inputConnection == null) {
                return;
            }
            InputConnection ic = inputConnection;
            ExtractedText extractedText = ic.getExtractedText(new ExtractedTextRequest(), 0);
            if (extractedText == null) {
                return;
            }
            ExtractedText extracted = extractedText;
            int next = RangesKt.coerceIn((int)(extracted.selectionEnd + delta), (int)0, (int)extracted.text.length());
            Object object = Result.constructor-impl((Object)ic.setSelection(next, next));
        }
        catch (Throwable throwable) {
            Object object = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)throwable));
        }
    }

    @Override
    public void onPressFeedback() {
        block20: {
            Object object;
            KeyboardPreferences keyboardPreferences = this.prefs;
            if (keyboardPreferences == null) {
                Intrinsics.throwUninitializedPropertyAccessException((String)"prefs");
                keyboardPreferences = null;
            }
            if (!keyboardPreferences.getKeySounds()) break block20;
            Object object2 = this;
            try {
                Object object3;
                SlashboardInputMethodService $this$onPressFeedback_u24lambda_u2419 = object2;
                boolean bl = false;
                KeyboardPreferences keyboardPreferences2 = $this$onPressFeedback_u24lambda_u2419.prefs;
                if (keyboardPreferences2 == null) {
                    Intrinsics.throwUninitializedPropertyAccessException((String)"prefs");
                    keyboardPreferences2 = null;
                }
                switch (keyboardPreferences2.getSoundPack()) {
                    case "ios": {
                        SoundPool soundPool = $this$onPressFeedback_u24lambda_u2419.soundPool;
                        if (soundPool != null) {
                            object3 = soundPool.play($this$onPressFeedback_u24lambda_u2419.soundIos, 1.0f, 1.0f, 1, 0, 1.0f);
                            break;
                        }
                        object3 = null;
                        break;
                    }
                    case "mechanical": {
                        SoundPool soundPool = $this$onPressFeedback_u24lambda_u2419.soundPool;
                        if (soundPool != null) {
                            object3 = soundPool.play($this$onPressFeedback_u24lambda_u2419.soundMech, 1.0f, 1.0f, 1, 0, 1.0f);
                            break;
                        }
                        object3 = null;
                        break;
                    }
                    case "typewriter": {
                        SoundPool soundPool = $this$onPressFeedback_u24lambda_u2419.soundPool;
                        if (soundPool != null) {
                            object3 = soundPool.play($this$onPressFeedback_u24lambda_u2419.soundType, 1.0f, 1.0f, 1, 0, 1.0f);
                            break;
                        }
                        object3 = null;
                        break;
                    }
                    default: {
                        Object object4 = $this$onPressFeedback_u24lambda_u2419.getSystemService("audio");
                        Intrinsics.checkNotNull((Object)object4, (String)"null cannot be cast to non-null type android.media.AudioManager");
                        ((AudioManager)object4).playSoundEffect(0, 0.35f);
                        object3 = Unit.INSTANCE;
                    }
                }
                object = Result.constructor-impl((Object)object3);
            }
            catch (Throwable bl) {
                object = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)bl));
            }
            object2 = object;
            Throwable throwable = Result.exceptionOrNull-impl((Object)object2);
            if (throwable == null) break block20;
            Object it = object = throwable;
            boolean bl = false;
            Object object5 = this.getSystemService("audio");
            Intrinsics.checkNotNull((Object)object5, (String)"null cannot be cast to non-null type android.media.AudioManager");
            ((AudioManager)object5).playSoundEffect(0, 0.35f);
        }
    }

    @Override
    public float languageScoreForKey(@NotNull String output) {
        KeyboardPreferences keyboardPreferences;
        String string;
        Intrinsics.checkNotNullParameter((Object)output, (String)"output");
        if (this.restricted || this.editorLayout != EditorLayout.TEXT) {
            return 0.0f;
        }
        if (output.length() == 1 && Character.isLetter(output.charAt(0)) && output.charAt(0) < '\u0080') {
            string = this.composition.getSource() + output;
            keyboardPreferences = this.prefs;
            if (keyboardPreferences == null) {
                Intrinsics.throwUninitializedPropertyAccessException((String)"prefs");
                keyboardPreferences = null;
            }
        } else {
            return 0.0f;
        }
        String next = SinhalaEngine.INSTANCE.transliterate(string, keyboardPreferences.getMode());
        PredictionRepository predictionRepository = this.prediction;
        return predictionRepository != null ? predictionRepository.prefixEvidence(next) : 0.0f;
    }

    @Override
    public void onPreviewDelete(int clusters) {
        String cluster;
        InputConnection inputConnection = this.getCurrentInputConnection();
        if (inputConnection == null) {
            return;
        }
        InputConnection ic = inputConnection;
        this.commitComposition();
        ExtractedText extractedText = ic.getExtractedText(new ExtractedTextRequest(), 0);
        if (extractedText == null) {
            return;
        }
        ExtractedText extracted = extractedText;
        if (this.deleteAnchor < 0) {
            this.deleteAnchor = extracted.selectionEnd;
        }
        CharSequence charSequence = ic.getTextBeforeCursor(256, 0);
        String string = charSequence != null ? ((Object)charSequence).toString() : null;
        if (string == null) {
            string = "";
        }
        String before = string;
        int consumed = 0;
        String text = before;
        for (int remaining = clusters; remaining > 0 && ((CharSequence)text).length() > 0 && !(((CharSequence)(cluster = GraphemeDelete.INSTANCE.lastCluster(text))).length() == 0); --remaining) {
            consumed += cluster.length();
            text = StringsKt.dropLast((String)text, (int)cluster.length());
        }
        this.deleteLength = consumed;
        SlashboardInputMethodService slashboardInputMethodService = this;
        try {
            SlashboardInputMethodService $this$onPreviewDelete_u24lambda_u2421 = slashboardInputMethodService;
            boolean bl = false;
            Object object = Result.constructor-impl((Object)ic.setSelection(RangesKt.coerceAtLeast((int)($this$onPreviewDelete_u24lambda_u2421.deleteAnchor - consumed), (int)0), $this$onPreviewDelete_u24lambda_u2421.deleteAnchor));
        }
        catch (Throwable throwable) {
            Object object = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)throwable));
        }
    }

    @Override
    public void onCommitPreviewDelete() {
        InputConnection ic = this.getCurrentInputConnection();
        if (ic != null && this.deleteLength > 0) {
            ic.commitText((CharSequence)"", 1);
        }
        this.deleteAnchor = -1;
        this.deleteLength = 0;
        this.precedingDirty = true;
        this.updateSuggestions();
    }

    @Override
    public void onCancelPreviewDelete() {
        InputConnection ic = this.getCurrentInputConnection();
        if (ic != null && this.deleteAnchor >= 0) {
            SlashboardInputMethodService slashboardInputMethodService = this;
            try {
                SlashboardInputMethodService $this$onCancelPreviewDelete_u24lambda_u2422 = slashboardInputMethodService;
                boolean bl = false;
                Object object = Result.constructor-impl((Object)ic.setSelection($this$onCancelPreviewDelete_u24lambda_u2422.deleteAnchor, $this$onCancelPreviewDelete_u24lambda_u2422.deleteAnchor));
            }
            catch (Throwable throwable) {
                Object object = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)throwable));
            }
        }
        this.deleteAnchor = -1;
        this.deleteLength = 0;
    }

    public boolean onKeyDown(int keyCode, @Nullable KeyEvent event) {
        if (event != null && event.isPrintingKey() && !event.isCtrlPressed() && !event.isAltPressed()) {
            this.onCharacter(String.valueOf((char)event.getUnicodeChar()));
            return true;
        }
        if (keyCode == 67) {
            KeyboardActions.DefaultImpls.onBackspace$default(this, false, 1, null);
            return true;
        }
        if (keyCode == 62) {
            this.onSpace();
            return true;
        }
        if (keyCode == 66) {
            this.onEnter();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private final String commitComposition() {
        String string;
        if (!this.composition.getActive()) {
            return null;
        }
        String it = string = this.composition.getRendered();
        boolean bl = false;
        String word = !StringsKt.isBlank((CharSequence)it) ? string : null;
        InputConnection inputConnection = this.getCurrentInputConnection();
        if (inputConnection != null) {
            inputConnection.finishComposingText();
        }
        this.composition.clear();
        StringsKt.clear((StringBuilder)this.slsSource);
        int n = this.generation;
        this.generation = n + 1;
        return word;
    }

    private final void clearLocalCompositionState() {
        this.composition.clear();
        StringsKt.clear((StringBuilder)this.slsSource);
        int n = this.generation;
        this.generation = n + 1;
        Future<?> future = this.predictionTask;
        if (future != null) {
            future.cancel(true);
        }
        this.precedingDirty = true;
        if (this.keyboard != null) {
            KeyboardView keyboardView = this.keyboard;
            if (keyboardView == null) {
                Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
                keyboardView = null;
            }
            keyboardView.setCandidates(CollectionsKt.emptyList());
        }
    }

    private final void cancelComposition(boolean removeHostText) {
        SlashboardInputMethodService slashboardInputMethodService = this;
        try {
            SlashboardInputMethodService $this$cancelComposition_u24lambda_u2424 = slashboardInputMethodService;
            boolean bl = false;
            if (removeHostText && ((CharSequence)$this$cancelComposition_u24lambda_u2424.composition.getRendered()).length() > 0) {
                InputConnection inputConnection = $this$cancelComposition_u24lambda_u2424.getCurrentInputConnection();
                if (inputConnection != null) {
                    inputConnection.deleteSurroundingText($this$cancelComposition_u24lambda_u2424.composition.getRendered().length(), 0);
                }
            }
            InputConnection inputConnection = $this$cancelComposition_u24lambda_u2424.getCurrentInputConnection();
            Object object = Result.constructor-impl((Object)(inputConnection != null ? Boolean.valueOf(inputConnection.finishComposingText()) : null));
        }
        catch (Throwable throwable) {
            Object object = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)throwable));
        }
        this.composition.clear();
        StringsKt.clear((StringBuilder)this.slsSource);
        int n = this.generation;
        this.generation = n + 1;
        Future<?> future = this.predictionTask;
        if (future != null) {
            future.cancel(true);
        }
        this.precedingDirty = true;
        if (this.keyboard != null) {
            KeyboardView keyboardView = this.keyboard;
            if (keyboardView == null) {
                Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
                keyboardView = null;
            }
            keyboardView.setCandidates(CollectionsKt.emptyList());
        }
    }

    private final void deleteFromHost(boolean word) {
        InputConnection inputConnection = this.getCurrentInputConnection();
        if (inputConnection == null) {
            return;
        }
        InputConnection ic = inputConnection;
        SlashboardInputMethodService slashboardInputMethodService = this;
        try {
            String before;
            SlashboardInputMethodService $this$deleteFromHost_u24lambda_u2425 = slashboardInputMethodService;
            boolean bl = false;
            CharSequence charSequence = ic.getTextBeforeCursor(word ? 256 : 32, 0);
            String string = charSequence != null ? ((Object)charSequence).toString() : null;
            if (string == null) {
                string = before = "";
            }
            if (word) {
                String target = GraphemeDelete.INSTANCE.lastWordSegment(before);
                boolean bl2 = ((CharSequence)target).length() > 0 ? ic.deleteSurroundingText(target.length(), 0) : ic.sendKeyEvent(new KeyEvent(0, 67));
                return;
            }
            String cluster = GraphemeDelete.INSTANCE.lastCluster(before);
            if (((CharSequence)cluster).length() == 0) {
                ic.sendKeyEvent(new KeyEvent(0, 67));
                return;
            }
            String reduced = GraphemeDelete.INSTANCE.reduceSlashboard(cluster);
            if (reduced == null) {
                ic.deleteSurroundingText(cluster.length(), 0);
                return;
            }
            ic.beginBatchEdit();
            ic.deleteSurroundingText(cluster.length(), 0);
            ic.commitText((CharSequence)reduced, 1);
            Object object = Result.constructor-impl((Object)ic.endBatchEdit());
        }
        catch (Throwable throwable) {
            Object object = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)throwable));
        }
    }

    private final void updateSuggestions() {
        block34: {
            String beforeString;
            MatchResult mathMatch;
            Object $this$updateSuggestions_u24lambda_u2426;
            block33: {
                block32: {
                    if (this.keyboard == null || this.restricted) break block32;
                    KeyboardPreferences keyboardPreferences = this.prefs;
                    if (keyboardPreferences == null) {
                        Intrinsics.throwUninitializedPropertyAccessException((String)"prefs");
                        keyboardPreferences = null;
                    }
                    if (keyboardPreferences.getSuggestions()) break block33;
                }
                if (this.keyboard != null) {
                    KeyboardView keyboardView = this.keyboard;
                    if (keyboardView == null) {
                        Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
                        keyboardView = null;
                    }
                    keyboardView.setCandidates(CollectionsKt.emptyList());
                }
                return;
            }
            Object object = this;
            try {
                $this$updateSuggestions_u24lambda_u2426 = object;
                boolean bl = false;
                Object object2 = $this$updateSuggestions_u24lambda_u2426.getCurrentInputConnection();
                $this$updateSuggestions_u24lambda_u2426 = Result.constructor-impl(object2 != null && (object2 = object2.getTextBeforeCursor(100, 0)) != null ? object2.toString() : null);
            }
            catch (Throwable bl) {
                $this$updateSuggestions_u24lambda_u2426 = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)bl));
            }
            object = $this$updateSuggestions_u24lambda_u2426;
            String string = (String)(Result.isFailure-impl((Object)object) ? null : object);
            if (string == null) {
                string = "";
            }
            if ((mathMatch = Regex.find$default((Regex)new Regex("([0-9]+(?:\\.[0-9]+)?)([\\+\\-\\*\\/])([0-9]+(?:\\.[0-9]+)?)=$"), (CharSequence)(beforeString = string), (int)0, (int)2, null)) == null || this.composition.getActive()) break block34;
            Double d = StringsKt.toDoubleOrNull((String)((String)mathMatch.getGroupValues().get(1)));
            double a = d != null ? d : 0.0;
            String op = (String)mathMatch.getGroupValues().get(2);
            Double d2 = StringsKt.toDoubleOrNull((String)((String)mathMatch.getGroupValues().get(3)));
            double b = d2 != null ? d2 : 0.0;
            double res = switch (op) {
                case "+" -> a + b;
                case "-" -> a - b;
                case "*" -> a * b;
                case "/" -> {
                    if (!(b == 0.0)) {
                        yield a / b;
                    }
                    yield 0.0;
                }
                default -> 0.0;
            };
            String formatted = res == (double)((long)res) ? String.valueOf((long)res) : String.valueOf(res);
            KeyboardView keyboardView = this.keyboard;
            if (keyboardView == null) {
                Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
                keyboardView = null;
            }
            keyboardView.setCandidates(CollectionsKt.listOf((Object)formatted));
            return;
        }
        if (!this.composition.getActive()) {
            this.precedingDirty = true;
        }
        String prefix = this.composition.getRendered();
        List<String> context = this.precedingWords();
        ++this.generation;
        int token = this.generation;
        Future<?> future = this.predictionTask;
        if (future != null) {
            future.cancel(true);
        }
        PredictionRepository currentPrediction = this.prediction;
        if (currentPrediction == null) {
            KeyboardView keyboardView = this.keyboard;
            if (keyboardView == null) {
                Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
                keyboardView = null;
            }
            keyboardView.setCandidates(CollectionsKt.emptyList());
            return;
        }
        EmojiRepository currentEmoji = this.emoji;
        this.predictionTask = this.executor.submit(() -> SlashboardInputMethodService.updateSuggestions$lambda$30(currentPrediction, prefix, context, this, currentEmoji, token));
    }

    private final List<String> precedingWords() {
        Object object;
        if (!this.precedingDirty && this.composition.getActive()) {
            return this.cachedPreceding;
        }
        Object object2 = this;
        try {
            SlashboardInputMethodService $this$precedingWords_u24lambda_u2431 = object2;
            boolean bl = false;
            Object object3 = $this$precedingWords_u24lambda_u2431.getCurrentInputConnection();
            object = Result.constructor-impl(object3 != null && (object3 = object3.getTextBeforeCursor(256, 0)) != null ? object3.toString() : null);
        }
        catch (Throwable throwable) {
            object = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)throwable));
        }
        object2 = object;
        String string = (String)(Result.isFailure-impl((Object)object2) ? null : object2);
        if (string == null) {
            string = "";
        }
        String before = string;
        String withoutComposing = ((CharSequence)this.composition.getRendered()).length() > 0 && StringsKt.endsWith$default((String)before, (String)this.composition.getRendered(), (boolean)false, (int)2, null) ? StringsKt.dropLast((String)before, (int)this.composition.getRendered().length()) : before;
        this.cachedPreceding = CollectionsKt.takeLast((List)SequencesKt.toList((Sequence)SequencesKt.map((Sequence)Regex.findAll$default((Regex)new Regex("[\\p{L}\\p{M}]+"), (CharSequence)withoutComposing, (int)0, (int)2, null), SlashboardInputMethodService::precedingWords$lambda$32)), (int)2);
        this.precedingDirty = false;
        return this.cachedPreceding;
    }

    private final void learn(String word) {
        block1: {
            CharSequence charSequence = word;
            if (charSequence == null || StringsKt.isBlank((CharSequence)charSequence) || this.restricted) {
                return;
            }
            String previous = this.previousCommittedWord;
            this.previousCommittedWord = word;
            LocalLearningStore localLearningStore = this.learning;
            if (localLearningStore == null) break block1;
            LocalLearningStore store = localLearningStore;
            boolean bl = false;
            this.executor.submit(() -> SlashboardInputMethodService.learn$lambda$34$lambda$33(store, word, previous));
        }
    }

    private final void captureClipboard() {
        SlashboardInputMethodService slashboardInputMethodService = this;
        try {
            Unit unit;
            SlashboardInputMethodService $this$captureClipboard_u24lambda_u2436 = slashboardInputMethodService;
            boolean bl = false;
            $this$captureClipboard_u24lambda_u2436.checkOtp();
            KeyboardPreferences keyboardPreferences = $this$captureClipboard_u24lambda_u2436.prefs;
            if (keyboardPreferences == null) {
                Intrinsics.throwUninitializedPropertyAccessException((String)"prefs");
                keyboardPreferences = null;
            }
            if (!keyboardPreferences.getClipboardHistory() || $this$captureClipboard_u24lambda_u2436.restricted || $this$captureClipboard_u24lambda_u2436.editorLayout != EditorLayout.TEXT) {
                return;
            }
            Object object = $this$captureClipboard_u24lambda_u2436.getSystemService("clipboard");
            ClipboardManager clipboardManager = object instanceof ClipboardManager ? (ClipboardManager)object : null;
            if (clipboardManager == null) {
                return;
            }
            ClipboardManager manager = clipboardManager;
            ClipData clipData = manager.getPrimaryClip();
            if (clipData == null) {
                return;
            }
            ClipData clip = clipData;
            if (clip.getItemCount() == 0) {
                return;
            }
            ClipboardHistoryStore clipboardHistoryStore = $this$captureClipboard_u24lambda_u2436.clipboardHistory;
            if (clipboardHistoryStore == null) {
                return;
            }
            ClipboardHistoryStore store = clipboardHistoryStore;
            CharSequence charSequence = clip.getItemAt(0).coerceToText((Context)$this$captureClipboard_u24lambda_u2436);
            if (charSequence != null && (charSequence = ((Object)charSequence).toString()) != null) {
                CharSequence p0 = charSequence;
                boolean bl2 = false;
                boolean $i$f$captureClipboard$lambda$36$stub_for_inlining$35 = false;
                store.add((String)p0);
                unit = Unit.INSTANCE;
            } else {
                unit = null;
            }
            Object object2 = Result.constructor-impl(unit);
        }
        catch (Throwable throwable) {
            Object object = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)throwable));
        }
    }

    private final void checkOtp() {
        KeyboardView keyboardView;
        if (this.keyboard == null) {
            return;
        }
        Object object = this.getSystemService("clipboard");
        ClipboardManager clipboardManager = object instanceof ClipboardManager ? (ClipboardManager)object : null;
        if (clipboardManager == null) {
            return;
        }
        ClipboardManager manager = clipboardManager;
        ClipData clip = manager.getPrimaryClip();
        boolean hasOtp = false;
        if (clip != null && clip.getItemCount() > 0) {
            CharSequence charSequence;
            String text;
            CharSequence charSequence2 = clip.getItemAt(0).getText();
            String string = text = charSequence2 != null ? ((Object)charSequence2).toString() : null;
            if (text != null && new Regex(".*\\b\\d{4,8}\\b.*").matches(charSequence = (CharSequence)text)) {
                hasOtp = true;
            }
        }
        if ((keyboardView = this.keyboard) == null) {
            Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
            keyboardView = null;
        }
        keyboardView.setOtpAvailable(hasOtp);
    }

    private final void listenForClipboard() {
        SlashboardInputMethodService slashboardInputMethodService = this;
        try {
            SlashboardInputMethodService $this$listenForClipboard_u24lambda_u2439 = slashboardInputMethodService;
            boolean bl = false;
            Object object = $this$listenForClipboard_u24lambda_u2439.getSystemService("clipboard");
            ClipboardManager clipboardManager = object instanceof ClipboardManager ? (ClipboardManager)object : null;
            if (clipboardManager == null) {
                return;
            }
            ClipboardManager manager = clipboardManager;
            manager.removePrimaryClipChangedListener($this$listenForClipboard_u24lambda_u2439.clipListener);
            KeyboardPreferences keyboardPreferences = $this$listenForClipboard_u24lambda_u2439.prefs;
            if (keyboardPreferences == null) {
                Intrinsics.throwUninitializedPropertyAccessException((String)"prefs");
                keyboardPreferences = null;
            }
            if (keyboardPreferences.getClipboardHistory() && !$this$listenForClipboard_u24lambda_u2439.restricted && $this$listenForClipboard_u24lambda_u2439.editorLayout == EditorLayout.TEXT) {
                manager.addPrimaryClipChangedListener($this$listenForClipboard_u24lambda_u2439.clipListener);
            }
            Object object2 = Result.constructor-impl((Object)Unit.INSTANCE);
        }
        catch (Throwable throwable) {
            Object object = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)throwable));
        }
    }

    private final void stopClipboardListener() {
        SlashboardInputMethodService slashboardInputMethodService = this;
        try {
            Unit unit;
            SlashboardInputMethodService $this$stopClipboardListener_u24lambda_u2440 = slashboardInputMethodService;
            boolean bl = false;
            Object object = $this$stopClipboardListener_u24lambda_u2440.getSystemService("clipboard");
            ClipboardManager clipboardManager = object instanceof ClipboardManager ? (ClipboardManager)object : null;
            if (clipboardManager != null) {
                clipboardManager.removePrimaryClipChangedListener($this$stopClipboardListener_u24lambda_u2440.clipListener);
                unit = Unit.INSTANCE;
            } else {
                unit = null;
            }
            Object object2 = Result.constructor-impl(unit);
        }
        catch (Throwable throwable) {
            Object object = Result.constructor-impl((Object)ResultKt.createFailure((Throwable)throwable));
        }
    }

    private final void rememberEmoji(String value) {
        KeyboardView keyboardView;
        this.recentEmoji.remove(value);
        this.recentEmoji.add(0, value);
        if (this.recentEmoji.size() > 32) {
            this.recentEmoji = CollectionsKt.toMutableList((Collection)CollectionsKt.take((Iterable)this.recentEmoji, (int)32));
        }
        if ((keyboardView = this.keyboard) == null) {
            Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
            keyboardView = null;
        }
        keyboardView.setRecentEmoji(this.recentEmoji);
    }

    private final void feedback() {
        KeyboardPreferences keyboardPreferences;
        KeyboardPreferences keyboardPreferences2 = this.prefs;
        if (keyboardPreferences2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException((String)"prefs");
            keyboardPreferences2 = null;
        }
        if (keyboardPreferences2.getHaptics() && this.keyboard != null) {
            int type = Build.VERSION.SDK_INT >= 27 ? 3 : 3;
            KeyboardView keyboardView = this.keyboard;
            if (keyboardView == null) {
                Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
                keyboardView = null;
            }
            keyboardView.performHapticFeedback(type);
        }
        if ((keyboardPreferences = this.prefs) == null) {
            Intrinsics.throwUninitializedPropertyAccessException((String)"prefs");
            keyboardPreferences = null;
        }
        if (keyboardPreferences.getKeySounds()) {
            Object object = this.getSystemService("audio");
            Intrinsics.checkNotNull((Object)object, (String)"null cannot be cast to non-null type android.media.AudioManager");
            ((AudioManager)object).playSoundEffect(0, 0.35f);
        }
    }

    private final boolean offerSystemSwitch() {
        boolean bl;
        if (Build.VERSION.SDK_INT >= 28) {
            bl = this.shouldOfferSwitchingToNextInputMethod();
        } else {
            Object object = this.getSystemService("input_method");
            Intrinsics.checkNotNull((Object)object, (String)"null cannot be cast to non-null type android.view.inputmethod.InputMethodManager");
            Window window = this.getWindow().getWindow();
            bl = ((InputMethodManager)object).shouldOfferSwitchingToNextInputMethod(window != null && (window = window.getAttributes()) != null ? window.token : null);
        }
        return bl;
    }

    private final void switchSystemKeyboard() {
        if (Build.VERSION.SDK_INT >= 28) {
            v0 = this.switchToNextInputMethod(false);
        } else {
            Object object = this.getSystemService("input_method");
            Intrinsics.checkNotNull((Object)object, (String)"null cannot be cast to non-null type android.view.inputmethod.InputMethodManager");
            Window window = this.getWindow().getWindow();
            v0 = ((InputMethodManager)object).switchToNextInputMethod(window != null && (window = window.getAttributes()) != null ? window.token : null, false);
        }
    }

    private static final Unit onCreate$lambda$2(SlashboardInputMethodService this$0, String text) {
        Intrinsics.checkNotNullParameter((Object)text, (String)"text");
        InputConnection inputConnection = this$0.getCurrentInputConnection();
        if (inputConnection != null) {
            inputConnection.commitText((CharSequence)(text + " "), 1);
        }
        this$0.updateSuggestions();
        return Unit.INSTANCE;
    }

    private static final Unit onCreate$lambda$3(SlashboardInputMethodService this$0, String text) {
        block0: {
            Intrinsics.checkNotNullParameter((Object)text, (String)"text");
            InputConnection inputConnection = this$0.getCurrentInputConnection();
            if (inputConnection == null) break block0;
            inputConnection.setComposingText((CharSequence)text, 1);
        }
        return Unit.INSTANCE;
    }

    private static final Unit onCreate$lambda$4(SlashboardInputMethodService this$0, int error) {
        InputConnection inputConnection = this$0.getCurrentInputConnection();
        if (inputConnection != null) {
            inputConnection.finishComposingText();
        }
        if (error == 9) {
            Toast.makeText((Context)((Context)this$0), (CharSequence)"Microphone permission required for voice input", (int)0).show();
        }
        return Unit.INSTANCE;
    }

    private static final Unit onCreate$lambda$5(SlashboardInputMethodService this$0) {
        Toast.makeText((Context)((Context)this$0), (CharSequence)"Listening...", (int)0).show();
        return Unit.INSTANCE;
    }

    private static final boolean onCharacter$lambda$9$lambda$8(int it) {
        return it > 126976;
    }

    private static final void updateSuggestions$lambda$30$lambda$28(int $token, SlashboardInputMethodService this$0, List $values) {
        if ($token == this$0.generation) {
            KeyboardView keyboardView = this$0.keyboard;
            if (keyboardView == null) {
                Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
                keyboardView = null;
            }
            keyboardView.setCandidates(CollectionsKt.take((Iterable)CollectionsKt.distinct((Iterable)$values), (int)3));
        }
    }

    private static final void updateSuggestions$lambda$30$lambda$29(int $token, SlashboardInputMethodService this$0) {
        if ($token == this$0.generation) {
            KeyboardView keyboardView = this$0.keyboard;
            if (keyboardView == null) {
                Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
                keyboardView = null;
            }
            keyboardView.setCandidates(CollectionsKt.emptyList());
        }
    }

    /*
     * WARNING - void declaration
     */
    private static final void updateSuggestions$lambda$30(PredictionRepository $currentPrediction, String $prefix, List $context, SlashboardInputMethodService this$0, EmojiRepository $currentEmoji, int $token) {
        try {
            List<String> emojis;
            KeyboardPreferences keyboardPreferences;
            void $this$mapTo$iv$iv;
            Iterable $this$map$iv = $currentPrediction.candidates($prefix, $context, 3);
            boolean $i$f$map = false;
            Iterable iterable = $this$map$iv;
            Collection destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault((Iterable)$this$map$iv, (int)10));
            boolean $i$f$mapTo = false;
            for (Object item$iv$iv : $this$mapTo$iv$iv) {
                void it;
                Candidate candidate = (Candidate)item$iv$iv;
                Collection collection = destination$iv$iv;
                boolean bl = false;
                collection.add(it.getText());
            }
            List values = CollectionsKt.toMutableList((Collection)((List)destination$iv$iv));
            if (SlashboardEasterEgg.INSTANCE.isCompleteTrueName($prefix, this$0.composition.getSource())) {
                values.add(0, "\u2726 \u0d85\u0d9a\u0dca\u0dc2\u0dbb");
            }
            if ((keyboardPreferences = this$0.prefs) == null) {
                Intrinsics.throwUninitializedPropertyAccessException((String)"prefs");
                keyboardPreferences = null;
            }
            List<String> list = emojis = keyboardPreferences.getEmojiSuggestions() && !StringsKt.isBlank((CharSequence)$prefix) && $currentEmoji != null ? $currentEmoji.search($prefix, 2, false) : CollectionsKt.emptyList();
            if (!((Collection)emojis).isEmpty()) {
                if (!((Collection)values).isEmpty()) {
                    values.add(Math.min(1, values.size()), CollectionsKt.first(emojis));
                } else {
                    values.add(CollectionsKt.first(emojis));
                }
            }
            this$0.main.post(() -> SlashboardInputMethodService.updateSuggestions$lambda$30$lambda$28($token, this$0, values));
        }
        catch (Throwable throwable) {
            this$0.main.post(() -> SlashboardInputMethodService.updateSuggestions$lambda$30$lambda$29($token, this$0));
        }
    }

    private static final String precedingWords$lambda$32(MatchResult it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        return it.getValue();
    }

    private static final void learn$lambda$34$lambda$33(LocalLearningStore $store, String $word, String $previous) {
        $store.record($word, $previous);
    }

    private static final void clipListener$lambda$38(SlashboardInputMethodService this$0) {
        block2: {
            this$0.captureClipboard();
            if (this$0.keyboard == null) break block2;
            ClipboardHistoryStore clipboardHistoryStore = this$0.clipboardHistory;
            if (clipboardHistoryStore != null) {
                ClipboardHistoryStore it = clipboardHistoryStore;
                boolean bl = false;
                KeyboardView keyboardView = this$0.keyboard;
                if (keyboardView == null) {
                    Intrinsics.throwUninitializedPropertyAccessException((String)"keyboard");
                    keyboardView = null;
                }
                keyboardView.setClipboardItems(it.items(), it.pinnedItems());
            }
        }
    }

    public static final /* synthetic */ void access$setLearning$p(SlashboardInputMethodService $this, LocalLearningStore localLearningStore) {
        $this.learning = localLearningStore;
    }

    public static final /* synthetic */ void access$setPrediction$p(SlashboardInputMethodService $this, PredictionRepository predictionRepository) {
        $this.prediction = predictionRepository;
    }

    public static final /* synthetic */ void access$setEmoji$p(SlashboardInputMethodService $this, EmojiRepository emojiRepository) {
        $this.emoji = emojiRepository;
    }

    public static final /* synthetic */ void access$setClipboardHistory$p(SlashboardInputMethodService $this, ClipboardHistoryStore clipboardHistoryStore) {
        $this.clipboardHistory = clipboardHistoryStore;
    }

    public static final /* synthetic */ KeyboardView access$getKeyboard$p(SlashboardInputMethodService $this) {
        return $this.keyboard;
    }

    public static final /* synthetic */ void access$updateSuggestions(SlashboardInputMethodService $this) {
        $this.updateSuggestions();
    }

    @Metadata(mv={2, 0, 0}, k=1, xi=48, d1={"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u000e\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007J\u0010\u0010\b\u001a\u00020\t2\b\u0010\u0006\u001a\u0004\u0018\u00010\u0007J\u0010\u0010\n\u001a\u00020\u000b2\b\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u00a8\u0006\f"}, d2={"Lorg/slashboard/ime/ime/SlashboardInputMethodService$Companion;", "", "<init>", "()V", "isRestrictedEditor", "", "info", "Landroid/view/inputmethod/EditorInfo;", "enterLabel", "", "editorLayout", "Lorg/slashboard/ime/ime/EditorLayout;", "app_debug"})
    public static final class Companion {
        private Companion() {
        }

        public final boolean isRestrictedEditor(@NotNull EditorInfo info) {
            Object[] objectArray;
            Intrinsics.checkNotNullParameter((Object)info, (String)"info");
            int cls = info.inputType & 0xF;
            int variation = info.inputType & 0xFF0;
            switch (cls) {
                case 2: 
                case 3: 
                case 4: {
                    return true;
                }
            }
            if (cls == 1 && SetsKt.setOf((Object[])(objectArray = new Integer[]{128, 144, 224, 32, 208, 16, 176})).contains(variation)) {
                return true;
            }
            return (info.imeOptions & 0x1000000) != 0 || (info.imeOptions & 0xFF) == 3;
        }

        @NotNull
        public final String enterLabel(@Nullable EditorInfo info) {
            String string;
            Integer n;
            EditorInfo editorInfo = info;
            Integer n2 = n = editorInfo != null ? Integer.valueOf(editorInfo.imeOptions & 0xFF) : null;
            int n3 = 2;
            if (n2 != null && n2 == n3) {
                string = "Go";
            } else {
                Integer n4 = n;
                n3 = 3;
                if (n4 != null && n4 == n3) {
                    string = "\u2315";
                } else {
                    Integer n5 = n;
                    n3 = 4;
                    if (n5 != null && n5 == n3) {
                        string = "Send";
                    } else {
                        Integer n6 = n;
                        n3 = 5;
                        if (n6 != null && n6 == n3) {
                            string = "Next";
                        } else {
                            Integer n7 = n;
                            n3 = 6;
                            string = n7 != null && n7 == n3 ? "Done" : "\u21b5";
                        }
                    }
                }
            }
            return string;
        }

        @NotNull
        public final EditorLayout editorLayout(@Nullable EditorInfo info) {
            if (info == null) {
                return EditorLayout.TEXT;
            }
            int cls = info.inputType & 0xF;
            int variation = info.inputType & 0xFF0;
            return switch (cls) {
                case 2 -> {
                    boolean signed;
                    boolean decimal = (info.inputType & 0x2000) != 0;
                    boolean v0 = signed = (info.inputType & 0x1000) != 0;
                    if (decimal && signed) {
                        yield EditorLayout.SIGNED_DECIMAL;
                    }
                    if (decimal) {
                        yield EditorLayout.DECIMAL;
                    }
                    if (signed) {
                        yield EditorLayout.SIGNED_NUMBER;
                    }
                    yield EditorLayout.NUMBER;
                }
                case 3 -> EditorLayout.PHONE;
                case 4 -> EditorLayout.DATETIME;
                case 1 -> {
                    switch (variation) {
                        case 32: 
                        case 208: {
                            yield EditorLayout.EMAIL;
                        }
                        case 16: {
                            yield EditorLayout.URI;
                        }
                        case 128: 
                        case 144: 
                        case 224: {
                            yield EditorLayout.ASCII;
                        }
                    }
                    yield EditorLayout.TEXT;
                }
                default -> EditorLayout.TEXT;
            };
        }

        public /* synthetic */ Companion(DefaultConstructorMarker $constructor_marker) {
            this();
        }
    }
}
