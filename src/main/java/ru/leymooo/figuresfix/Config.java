package ru.leymooo.figuresfix;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
@SuppressWarnings({"Duplicates", "unchecked"})
public class Config {

    protected static DumperOptions yamlOptions = new DumperOptions() {{
        setWidth(80 * 2);
        setDefaultFlowStyle(FlowStyle.BLOCK);
     }};

    protected static Yaml yaml = new Yaml(yamlOptions);

    private File file;
    private Map<String, Object> map;

    private String description = "";

    /**
     * Загрузить
     *
     * @param data
     */
    public Config(String data) {
        this.loadFromString(data);
    }

    /**
     * Загрузить конфиг
     *
     * @param file текстовый файл формата yaml
     */
    public Config(File file) {
        this.file = file;
        this.reload();
    }

    /**
     * Получить или вставить новое значение в конфиг
     *
     * @param path путь
     * @param def  значение по уполчанию
     * @return значние их конфига или значение по умолчанию
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrSet(String path, T def) {
        if (!this.contains(path)) {
            this.setAndSave(path, def);
            return def;
        } else {
            return (T) this.get(path);
        }
    }

    /**
     * Получить или вставить новое значение в конфиг типа <code>? extends Number</code><br>
     * Добавлено в связи с тем, что часто возникают прроблемы с ClassCastException (int -> long, double - float)
     *
     * @param path путь
     * @param def  значение по уполчанию
     * @return number. из него можно получить любой формат числа:
     * <ul>
     * <li>{@link Number#intValue()}</li>
     * <li>{@link Number#longValue()}</li>
     * <li>{@link Number#doubleValue()}</li>
     * </ul>
     */
    public Number getOrSetNumber(String path, Number def) {
        if (!this.contains(path)) {
            this.setAndSave(path, def);
            return def;
        } else {
            return this.getNumber(path);
        }
    }

    /**
     * Получить значение типа <code>? extends Number</code><br>
     * Добавлено в связи с тем, что часто возникают прроблемы с ClassCastException (int -> long, double - float)
     *
     * @param path путь
     * @return number. из него можно получить любой формат числа:
     * <ul>
     * <li>{@link Number#intValue()}</li>
     * <li>{@link Number#longValue()}</li>
     * <li>{@link Number#doubleValue()}</li>
     * </ul>
     */
    public Number getNumber(String path) {
        return (Number) this.get(path);
    }

    public File getFile() {
        return file;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public String getString(String path) {
        Object o = this.get(path);
        return o == null ? null : o.toString();
    }

    public Object get(String path) {
        Object current = this.map;
        for (String next : path.split("\\.")) {
            current = ((Map) current).get(next);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    public void setIfNotExist(String path, Object o) {
        if (!this.contains(path)) {
            this.setAndSave(path, o);
        }
    }

    public void setAndSave(String path, Object o) {
        this.set(path, o);
        this.save();
    }

    public boolean contains(String path) {
        try {
            Object current = this.map;
            for (String next : path.split("\\.")) {
                current = ((Map) current).get(next);
            }
            return current != null;
        } catch (NullPointerException | ClassCastException e) {
            return false;
        }
    }

    public void set(String path, Object o) {
        Map<String, Object> current = this.map;
        String[] data = path.split("\\.");
        for (int i = 0; i < data.length - 1; i++) {
            String next = data[i];
            Map<String, Object> old = current;
            current = (Map) current.get(next);
            if (current == null) {
                old.put(next, current = new LinkedHashMap<>());
            }
        }
        if (o != null) {
            current.put(data[data.length - 1], o);
        } else {
            current.remove(data[data.length - 1]);
        }
    }

    public boolean getBoolean(String path) {
        return (boolean) this.get(path);
    }

    public void save() {
        if (file != null) {
            try {
                FileOutputStream stream = new FileOutputStream(file);
                stream.write(this.saveDataToString().getBytes());
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String saveDataToString() {
        StringBuilder builder = new StringBuilder();
        if (!description.isEmpty()) {
            builder.append('#').append(description.replace("\n", "\n#")).append("\n");
        }
        builder.append(this.saveToString());
        return builder.toString();
    }

    public String saveToString() {
        return yaml.dumpAs(this.map, null, DumperOptions.FlowStyle.BLOCK);
    }

    public void reload() {
        if (file == null) {
            throw new NullPointerException("конфиг не был загружен с файла, перезагрузить его нельзя");
        }
        this.createFile();
        this.loadFromFile();
    }

    private void loadFromFile() {
        String content = this.loadDataFromFile();
        this.loadFromString(content);
    }

    private void loadFromString(String data) {
        StringBuilder dataBuild = new StringBuilder();
        StringBuilder descBuild = new StringBuilder();
        for (String line : data.split("\n")) {
            if (!line.isEmpty()) {
                if (line.charAt(0) == '#') {
                    descBuild.append(line.substring(1)).append("\n");
                } else {
                    dataBuild.append(line).append("\n");
                }
            }
        }
        this.description = StringUtils.substring(descBuild.toString(), 0, -1);

        this.map = (Map<String, Object>) yaml.load(dataBuild.toString());
        if (map == null)
            map = new LinkedHashMap<>();
    }

    private String loadDataFromFile() {
        try {
            return FileUtils.readFileToString(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createFile() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getDescription() {
        return description;
    }

    /**
     * Вставить комментарий конфига<br>
     * Он будет виден в самом верху конфига<br>
     *
     * @param description строка с описанием, можно использовать переносы - '\n'
     */
    public void setDescription(String description) {
        if (!this.description.equals(description)) {
            this.description = description;
            this.save();
        }
    }

    public List<Object> getList(String path) {
        return (List<Object>) this.get(path);
    }

    public List<String> getStringList(String path) {
        final List<Object> list = this.getList(path);
        return list == null ? null : list.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * Получить все параметры в указанной секции
     *
     * @param section путь к секции
     * @return список всех параметров из этой секции
     */
    public Set<String> getKeys(String section) {
        final Map<String, Object> map = (Map) this.get(section);
        return map == null ? Collections.emptySet() : new LinkedHashSet<>(map.keySet());
    }

    public Integer getInt(String path) {
        return this.getNumber(path).intValue();
    }
}
