import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;

/**
 * 资源路径工具类
 * 确保所有资源加载限定在本地仓库文件夹内
 */
public class ResourceUtil {
    private static String resourceBasePath;

    /**
     * 获取资源文件夹的基础路径
     * 自动检测项目根目录（包含 resources/ 文件夹的目录）
     */
    public static String getResourceBasePath() {
        if (resourceBasePath != null) {
            return resourceBasePath;
        }

        // 尝试通过类文件位置检测项目根目录
        // 类文件位于: {项目根}/out/classes/
        // 因此 ../.. 就是项目根目录
        try {
            CodeSource codeSource = ResourceUtil.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                File classLocation = new File(codeSource.getLocation().toURI());
                if (classLocation.isFile()) {
                    // 运行在 JAR/classes 文件中: {项目根}/out/classes/
                    resourceBasePath = classLocation.getParentFile().getParentFile().getParent() + File.separator;
                } else {
                    // IDE 运行环境
                    resourceBasePath = classLocation.getParentFile().getParentFile().getParent() + File.separator;
                }
            }
        } catch (URISyntaxException e) {
            // 回退到当前工作目录
        }

        // 如果检测失败，使用当前工作目录
        if (resourceBasePath == null) {
            resourceBasePath = System.getProperty("user.dir") + File.separator;
        }

        // 验证 resources 文件夹存在
        File resourcesDir = new File(resourceBasePath + "resources");
        if (!resourcesDir.exists() || !resourcesDir.isDirectory()) {
            // 如果找不到 resources，尝试上一级目录
            File parent = new File(resourceBasePath).getParentFile();
            if (parent != null && new File(parent, "resources").exists()) {
                resourceBasePath = parent.getAbsolutePath() + File.separator;
            }
        }

        return resourceBasePath;
    }

    /**
     * 构建资源文件的完整路径
     * @param relativePath 相对于 resources/ 的路径，例如 "image/player.png"
     */
    public static String getResourcePath(String relativePath) {
        return getResourceBasePath() + "resources" + File.separator + relativePath;
    }

    /**
     * 构建音效文件的完整路径
     * @param fileName 音效文件名，例如 "GameBegin.wav"
     */
    public static String getSoundPath(String fileName) {
        return getResourceBasePath() + "resources" + File.separator + "sound" + File.separator + fileName;
    }

    /**
     * 构建图片资源的完整路径
     * @param fileName 图片文件名，例如 "icon.png"
     */
    public static String getImagePath(String fileName) {
        return getResourceBasePath() + "resources" + File.separator + "image" + File.separator + fileName;
    }
}