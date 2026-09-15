package com.pkmobile.keyboard.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile ShortcutDao _shortcutDao;

  private volatile PresetDao _presetDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(5) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `presets` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `source_type` TEXT NOT NULL, `is_active` INTEGER NOT NULL, `shortcut_count` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `shortcuts` (`preset_id` TEXT NOT NULL, `trigger_code` TEXT NOT NULL, `expansion_text` TEXT NOT NULL, `category` TEXT, `expansion_mode` TEXT NOT NULL, `package_name` TEXT NOT NULL, `is_active` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`preset_id`, `trigger_code`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shortcuts_preset_id` ON `shortcuts` (`preset_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shortcuts_trigger_code` ON `shortcuts` (`trigger_code`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '09af8429e7f4c6c9ecacc160f9f804ce')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `presets`");
        db.execSQL("DROP TABLE IF EXISTS `shortcuts`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsPresets = new HashMap<String, TableInfo.Column>(6);
        _columnsPresets.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPresets.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPresets.put("source_type", new TableInfo.Column("source_type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPresets.put("is_active", new TableInfo.Column("is_active", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPresets.put("shortcut_count", new TableInfo.Column("shortcut_count", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPresets.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPresets = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPresets = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPresets = new TableInfo("presets", _columnsPresets, _foreignKeysPresets, _indicesPresets);
        final TableInfo _existingPresets = TableInfo.read(db, "presets");
        if (!_infoPresets.equals(_existingPresets)) {
          return new RoomOpenHelper.ValidationResult(false, "presets(com.pkmobile.keyboard.data.db.PresetEntity).\n"
                  + " Expected:\n" + _infoPresets + "\n"
                  + " Found:\n" + _existingPresets);
        }
        final HashMap<String, TableInfo.Column> _columnsShortcuts = new HashMap<String, TableInfo.Column>(8);
        _columnsShortcuts.put("preset_id", new TableInfo.Column("preset_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShortcuts.put("trigger_code", new TableInfo.Column("trigger_code", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShortcuts.put("expansion_text", new TableInfo.Column("expansion_text", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShortcuts.put("category", new TableInfo.Column("category", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShortcuts.put("expansion_mode", new TableInfo.Column("expansion_mode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShortcuts.put("package_name", new TableInfo.Column("package_name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShortcuts.put("is_active", new TableInfo.Column("is_active", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShortcuts.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysShortcuts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesShortcuts = new HashSet<TableInfo.Index>(2);
        _indicesShortcuts.add(new TableInfo.Index("index_shortcuts_preset_id", false, Arrays.asList("preset_id"), Arrays.asList("ASC")));
        _indicesShortcuts.add(new TableInfo.Index("index_shortcuts_trigger_code", false, Arrays.asList("trigger_code"), Arrays.asList("ASC")));
        final TableInfo _infoShortcuts = new TableInfo("shortcuts", _columnsShortcuts, _foreignKeysShortcuts, _indicesShortcuts);
        final TableInfo _existingShortcuts = TableInfo.read(db, "shortcuts");
        if (!_infoShortcuts.equals(_existingShortcuts)) {
          return new RoomOpenHelper.ValidationResult(false, "shortcuts(com.pkmobile.keyboard.data.db.ShortcutEntity).\n"
                  + " Expected:\n" + _infoShortcuts + "\n"
                  + " Found:\n" + _existingShortcuts);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "09af8429e7f4c6c9ecacc160f9f804ce", "9e235dbaabf84a2158f612cc38e93af3");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "presets","shortcuts");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `presets`");
      _db.execSQL("DELETE FROM `shortcuts`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ShortcutDao.class, ShortcutDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PresetDao.class, PresetDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ShortcutDao shortcutDao() {
    if (_shortcutDao != null) {
      return _shortcutDao;
    } else {
      synchronized(this) {
        if(_shortcutDao == null) {
          _shortcutDao = new ShortcutDao_Impl(this);
        }
        return _shortcutDao;
      }
    }
  }

  @Override
  public PresetDao presetDao() {
    if (_presetDao != null) {
      return _presetDao;
    } else {
      synchronized(this) {
        if(_presetDao == null) {
          _presetDao = new PresetDao_Impl(this);
        }
        return _presetDao;
      }
    }
  }
}
