package org.marj4n.smooth_classes.integration;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;

import java.util.List;
import java.util.Optional;

/** Thin integration boundary. Puffish Skills remains authoritative for tree UI, points and unlock state. */
public final class PuffishSkillsIntegration {
    public static final Identifier TREE = id("tree");
    public static final Identifier ASCENDANCY = id("ascendancy");
    public static final Identifier AVENGER = id("avenger");
    public static final Identifier FOREIGNER = id("foreigner");
    public static final Identifier CASTER = id("caster");
    public static final Identifier BERSERKER = id("berserker");
    public static final Identifier ARCHER = id("archer");
    public static final Identifier ASSASSIN = id("assassin");
    public static final Identifier SABER = id("saber");
    public static final Identifier RULER = id("ruler");

    public static final List<Identifier> CLASS_CATEGORIES = List.of(
            AVENGER, FOREIGNER, CASTER, BERSERKER, ARCHER, ASSASSIN, SABER, RULER
    );

    private PuffishSkillsIntegration() {}
    private static Identifier id(String path) { return new Identifier("smooth_classes", path); }

    public static Optional<Category> category(Identifier id) { return SkillsAPI.getCategory(id); }

    public static boolean isCategoryUnlocked(Identifier categoryId, LivingEntity entity) {
        if (!(entity instanceof ServerPlayerEntity player)) return false;
        return category(categoryId).map(c -> c.isUnlocked(player)).orElse(false);
    }

    public static boolean isSkillUnlocked(Identifier categoryId, String skillId, LivingEntity entity) {
        if (!(entity instanceof ServerPlayerEntity player)) return false;
        return category(categoryId)
                .flatMap(c -> c.getSkill(skillId))
                .map(s -> s.getState(player) == Skill.State.UNLOCKED)
                .orElse(false);
    }


    /** Exact Continued ascendancy scaling: every unlocked skill in the
     *  ascendancy category contributes one point, including stat nodes. */
    private static final String[] ASCENDANCY_SKILLS = {
                "xpfympjreeee2xg3", "19feeqjl8v2tw7qs", "684vl5wk1uh39tgs", "j29h5uttpvxwtsrl", "uz120cj1eh3m47f1", "8vd2keep69wmz91q", "m9rtlf3ie21632pa", "9b93chqgrmfdvu5h",
                "9064wv4osejn9vzq", "bjfg89nyaq4yhsfq", "krttlk8a0erykrg2", "hyzzdcs232x5o6t3", "m7s2vzpgvkr0jef4", "jiyfn14al0mfsj90", "cw3q782at00vfx8b", "eokonv3bjkkkpbg3",
                "167zlhsdsxhyb71r", "ygbccx0tk1u6orm1", "kzp8nsq7x232nykl", "gep7tkyef4nfwbx3", "7fxs7w7nh0jn6plt", "6dpoq8p4ewe93lhc", "ultfbwb57iotzlhs", "c8mfnidef3t2qhpq",
                "cyb46ysrc9rp25kt", "i3ddwq3dx48sgviz", "zurchleiuxx5ssl3", "c97xjm2qfltwb6xh", "mzfp99b24hklba2q", "edz90hgd98sovi2g", "zjj0mu5dlsa9kkt6", "00hgt2rt34f3k2j4",
                "wr0wx9t1riof1p0k", "s5hblp5r9qhynv2d", "xapth623oxd4rqf3", "11rposb0oo5yrelt", "7aszq5ogeh93bg37", "b9q55z0bij5gqw5s", "pr61or8khwzp8jvs", "qeptdy1hfkldb0ju",
                "2i5uz630y0xfdbjy", "y2s87crgvdq2lo4f", "0x9tqth2hmd542su", "qn388iitc921lw17", "g7u6nse9yumg9gs0", "5esx7kump9zrj3xc", "lhia16snkia14ro5", "f7l2wbwumtkwivqz",
                "c2pqi5gvwlprh1xy", "du2c4o41qtgjc3nj", "je1is02s1pj4bt53", "bi3t15vjo0cekrdw", "i52m169og8pp2g4y", "mg4ovk8uoa9jcemx", "d9wf35km70rossne", "qdfmry8z5vo32b7m",
                "15cs0ok5h9p0ee2n", "y90ecz1zzm96h4v4", "pp75ftr0tcjtmatg", "fpulny5n0t5j4yyg", "hnqi66kv0vht472f", "b1e3uyw5lju5jjqy", "9sd7t7hkeniqbyhn", "iydfw8wohvm2ri5v",
                "ec1qh3gnf34pe8b0", "28mro1nf3mzz89k7", "jtauvjxfg0av67me", "al8rr4oc8tju373x", "rvilbol9qp4ra1jd", "gmmycznjg3wcm0dz", "87j2bqhk5tp5h3cx", "4lz3q8cqlaetorfg",
                "m12o8ts4llbudct7", "q4j5blx7jc8fdg1v", "zmmollo11bhjcp5p", "fvfaogtjeqi8vrrd", "97g7v1752xattalg", "6415cmq6oduqn0j0", "jg3s2xdtrdnxazxy", "compf96i5frhyvjw",
                "xw9is5u7pcytuodp", "2djvy1cicfqsbdmy", "3bk7iefb4x3mqgdd", "d19aze1xk00if9tj", "ampt8xg1hxkzfa2n", "g46zb84pcfg4hwrt", "mi90lwauwf7ym8nd", "6x5o5q944go05xq4",
                "3bl60xctgbikgkvd", "so77822ftgahpeyi", "rtxjhg12x0ffjrv8", "7p084bt30bi4ugm6", "g9y8aupnplp0ekwj", "z9qadxur856l17yh", "1ll8n5olcd4ehc24", "shqiws9rjyk39oie",
                "md8ppgrea60xtc6z", "gxeandvbnaphqw33", "r8hitxu9dqzjdu9e", "hk01e7b6tb8jguve", "s5q3o0rx99qzj8d0", "mtyf0ql0g672dz48", "hzq62i672ojlvhnw", "4aww0ijhiob4ozzb",
                "qbsxyexgoupv9i06", "x0y0juxr8983ew6v", "6cv335463t25lmvg", "y8mwxemx9ud7kjk5", "s3xz0rvpvjostqj1", "9cfpmu5dp1c0awmk", "jbbpglvwtw85d26u", "5fbryf2bqb7mvbwc",
                "z85sce0dapyacq6f", "uvku1t04b8nomk18", "3st8r3770i0jlomn", "mfuzdxswmww5j5wx", "av3gacoorigwpu5t", "2i7w6evl4eps9jku", "4w4ft6n2cvh50pvr", "6kfv7dygz1u4s3yr",
                "2b5tbwp9hqgw2j4d", "j8q3fthua3gaaui6", "pisuqypq3dgwz2m8", "nuzzislzkqexrx0e", "12ef23btgj6x1z3z", "sx7wihvrfsbuvk3l", "xdp6kezmnglf7tgg", "flxde0okpetkaion",
                "054mr9cs8kcncjyj", "vs9oj7wassqyq188", "2eyaugeepy9i1so9", "0hbjosi456e263q3", "w3jr0ry8gdxubk4s", "x01r1loblihamyk0", "wdg49dzeyo8rxrr0", "okakdtglu0fqac75",
                "0ep9bqj3wfct8fvm", "0rhteinc4ld0m9p6", "jxx8kk4mmvqxjalf", "hsmir570obqakzgo", "ubdbqoso8ram5kzk", "l89cmuhrd7zuxl8j", "mojxk5bzcd1x5o91", "dkr5z6js65uqm02s",
                "dcszetltp8w13lj9", "233tlg79rkgwzugi", "519g8bugsli2ceb3", "v45pcjcyhr7mgjxl", "b7kfcbhfl66mstcm", "07i5ntn0sufivy5w", "g47rxzgh0rb1m20e", "aqyrjkvci7otzsng",
                "6rit3sk2vqv72y60", "54ih1pqptjaza3yz", "wf9gkm1yeqpsni94", "j6kigo7l7lva4sw1", "6704h1vqec9k5si9", "eilb8pp6x8xm55q0", "0d4t140xi2w4f01l", "hgmwd99qrhz0pytx",
                "csw8h6s6so84fzkv", "2xyf3744gec3xinl", "yn1z6cj7vg1rjdvf", "x9i5yrhrx3refv8p", "xbpm55fw57jgx6gr", "nbap2bsohmc6vd2p", "nkjfvveh2qcp27uq", "m1mzv993u307mmc1",
                "mucivvpkmir8poln", "z463b1hvamt4e55e", "jzc0ssqgqq8j8thd", "dj3dl7a1yyj78ny7", "zypb6heyvtit3gzl", "hfgv8mma0wefge0s", "jf0fgp2gjkec36ub", "f6nv6bsdnk16sziq",
                "own2n2hml9a1a75q", "13ixe4n5jmw9ygss", "kfows38iteiqk4fx", "okc3y22lus3aw0ry", "iyy3nbakdqyvp24s", "s8heo8exjg49wxth", "xoncp2ka2f15v88p", "jkvetztt0e8svr3m",
                "adkoyqtc8ucy6tw6", "9l1y5rki9ho7avgy", "un35ucldp8qrzfj0", "327cp6h59cpnmdqs", "72am23v464y2uk2i", "oucsyf2bggdnm3ns", "ctuknjhh1x46bjhh", "gyhpn76o65akudx0",
                "tq889hsbli8oe1qh", "ni29x0qwkku1j3j8", "0wpgmjiytemne7zs", "q5xkhswelu8icov1", "cueui42wy8ypdlh5", "1stfx57ydk5z3nlb", "yjcp8nantj9uorpz", "5ktobf30wlpb1o51",
                "k4xq6ojyff9ynekm", "8fgn9nqec9g1p3ru", "mp3hn9oro8n4mkvs", "upp8npehy3a5fnuk", "ayfuy3por64z3n7l", "mbgmip3g1cipgl5p", "qfb3ivx59biz6fsh", "xfdqn0f93obcoxqc",
                "fi3wewhzb1tvgt3r", "oy4q2ct0xq3b8k2m", "1rehlmv41w9qejl0", "imwu3oa1ath2eqq3", "3iud0hafvnoltpht", "6zyitht00n6so1it", "22uofq24aeioquqy", "lyr5l9bon95lw0qb",
                "24mrafw4vbszyzi5", "9ke8lkr4f1yopfnu", "8ar7dnh49sa4ndl0", "ofaoput52v11ybmn", "ubr519aiecz6v1ip", "dvscd8q0985cex2c", "7n8ewfysqmb1xdaz", "bl6hj04pxj10xiii",
                "4lk3dahia1t8xj4c", "o5yg6r1e628e1rl8", "j3yde8xb55ygt0k4", "zh268xels7r7ompu", "7u7xmofsdan9fhi9", "c2t0mh78n8mfxvqg", "ydog9i6ylalklyec", "atqzsfydwfgqr2lp",
                "dfdbgbbyaav8f3w4", "bbb1qply8yfkgqy4", "lvqoxfz6fe2l6css", "eb9vzxuslluurwe7", "12eut0xekvtlcpbq", "10zro4hzv77re1yu", "17fcbkzphh3jgetv", "zgk73bap6exeds6i",
                "0nu5uui3plvnka44", "ehuiwn8agt3pxipp", "ofzm3wcjwyd1j7gt", "72h8yydgeyundgud", "8hic53ft4gxas1rw", "mvftx8pmbivykqdz", "9k3inifnoced1qhp", "hia1th88sml3600e",
                "47l122v0ryeik0sx", "zs9cw8v07f4vdx3u", "hggy9k6wgfpeljs9", "68whqy6myq0k553y", "hkz0t0joxyt1xqk6", "apf64krc36q4y6mu", "q9cif6eze9f0l14h", "1h6w6p6toync7wl1",
                "5nxf666hzmythyjg", "5blx30nc5tsp4mm6", "1x01cxvdwjjj72hc", "cb2ut72sy04b42mj", "mdyurisbkdjr85nh", "jy8sea39b568djj3", "hqg1gjurf33wfh1a", "7mvtx8hl2qq6m99g",
                "cw2xsc8e3l3c68c2", "jr4g7qaoqgb3rtua", "io1ec3z2irvk2wgu", "0j3jv5jjg4uqg5sh", "uv10ai4jdnt3pk1e", "st4obiva7e0ighjn", "9y1c4bi211ta0tz5", "ukvh4x41gevl36mn",
                "nwpjzx64zch6vgqj", "knaapdbgfevz1xek", "rmdtrggutia1dy9v", "m65y790wn8v1bchi", "opl1ekkz34qdmcmg", "mviihlnwmt4acw9t", "vdleesif7u1l307b", "o35f680eydw6rgw4",
                "uichn75rstz8as10", "1t5lph84bhgbcb4h", "s2ad23yfztk98t48", "h49x2wnbyyym4keu", "e39n11r46m8qa28q", "6msfyx3ss9ehhuuo", "8eg5ziuhuimsbqtk", "9xbundl3r07s432c",
                "bpqzal36gfhao9w2", "xt0lsnhw7n8v2cnp", "emrmq4m5t7stbfdv", "b03vamowbq0hlvv3", "euf6cw2itq8oo4b4", "xnat7n8rok23erkx", "y46d5smmshxa2kzm", "x7y0e8qa0l98k248",
                "fhnzop15t5aomwrn", "utgfdwg8xdqwf9y7", "vc48jjmyfsxbthpj", "jxel13k8bewrvxiv", "0e29bzkyruczy6l6", "pyd721myddgzim92", "ok0zc59lba3r3aiw", "badj6etm7yct3dei",
                "x2b65ki513yoj1a1", "2tqq6aj53ndmsuo4", "0qf6gzrhiz86vx8d", "nttsejzg4aajfbe3", "haafca8mma0673xv", "yg6jwd45mcwg8l6b", "o0pd7o05jqzv8bb1", "53gqaoif6f06jqb8",
                "da2d65j9429fu1hf", "09ucgbcpgxos6jvn", "i0nmn4qeu8fhwyvm", "xbtvfyoosmagld3w", "pcb122ymc3xhgcmn", "dziouw4x6joeuoek", "wmg9nukczhiwhfr7", "dvhek9z2af57f873",
                "qubyqwpd6kch4kon", "2q2en3aym6rfytlb", "ig4hz9j7zitlk026", "v8xw0ugt76eiqrgy", "zs74z7mu2h4yf314", "whbcrpstuzvn9ks5", "wiyuy08cjpe9g4or", "4rq6t6x1c0szrjkk",
                "81ixe0i0c1yli6zw", "8ki3rvrli1uq7hbo", "okkedm2qnigw6305", "w4fgm8if99pdmiss", "hsfxq9kd9zhfs36n", "9usd8fi0qpij93oj", "mqak278tknuhwaxz", "0kw94bci901w8l4d",
                "p9zchczzl66etsko", "w3wehpkik6z1q2it", "9iobk83q316ophs2", "qruxkduqk1s73hox", "9x4mrcqws29dfwkd", "kramrrr2jeye1tmu", "jjy7n0hllzv9tj2x", "yj3zoyeok16iyfet",
                "s4mf4ryi27uukfhf", "trddr4u5761u8mtc", "mc38iqvxjx68p3kb", "gbbju4j63hlbnwtb", "dxe3p1p1g3g1uxcc", "01pmb2uuijfemp5d", "85vww6r4k72t4isa", "2667qy4wo6fqm598",
                "u7ho30i7x7dn24uk", "exizs29lvalv7iht", "6qtf9zomkv6h4fas", "taj0cfsudcazrxd5", "0lnyglvciwy2ljjj", "o3uo42rwfji4iha9", "rs8gzhetong16zxq", "mkusldnzstcmwp4a",
                "d4b0jx6sc1h04nch", "s0fioki5gpg9dhe0", "ogwx6li28qquxzu0", "4fmq2hkdjz15by41", "5p38skntbhlx8yv1", "ic4ivwb9o5bqijk6", "d3m0h9gj18ba2aw0", "pvz6dbvm8j2ucq2i",
                "dd6267oemq3ew724", "v2yhrujafkx28k14", "wuzrrlwqvidwwzyv", "7eklagq9hxp5isoq", "3le5jm275ecya46c", "m0sw6l6t2usnkgyb", "46x6lipv2hti36u1", "55bqnfxqrkyfdovy",
                "j2c0wa6q3d8j68tz", "1dzxnuw2yr893ta5", "l1awux893zdk8g9x", "8hnc5kmud9gk5903", "xb1llwacrfs5fkht", "3v7zozyvu4zbne2v", "btmtc1pi5f2vssa5", "tkh7fixgjqwyvhjw",
                "zgoxx326sfnhdshb", "azc5cwj4ac74xwwl", "hccir2dx34esqovb", "v0okfv7uvpvo0w6n", "k9ri2fpfkvy5ecsk", "g8efg99qkn5dig65", "014cgqxw1qawivdi", "9r7gd8ypwgpbywtp",
                "dxx4ksi3sr4bgdcs", "umunmyry09bmugn7", "idvp2h9zxdbg82ww", "cwk8h2uqv7ggaacu", "u6r8wihqzi9vcsz3", "zjqtari5065lqtos", "0di60ir4phe5j3r5", "sy6b8okkwg6g92hj",
                "vzgzgbxl9zt33xhv", "jtrsbjmj5e1oczwq", "3tbeard1tux46gm8", "8ghp7y581nzpqanz", "43vbz4nnvrf057bp", "xv5qdihsl7nejldj", "z7j0qlcqmz7xbovg", "eydphwr30d9qggxf",
                "oef3xpobx62c9d94", "dacuo83q4svpq31c", "wjf0pxxki94oftcv", "agks2znp11w4d0eu", "sxbf4j76vnt7m3ke", "o9ckqqgn2199a1gd", "ztvbts0ftc58rcpp", "z674ndg9wtjdvea1",
                "77e8dp81pzmtdz8a", "an5htx8gn6h64rae", "8847p9p3133a8u24", "8zow47jovzoq4rmj", "skyoxtc7mega1tmg", "23x1tlxr5r3ivqhm", "z3y4uxnmpskdyazl", "dx2cn0e339w6x0nx",
                "mcffu16kll2ym9wg", "7radrs6nwu2pzjqr", "lxkgkjdukq7f5uyc", "xym8bgp9or5e51qr", "gcoww89mevdyqg70", "ujx3txhfy8czakcx", "t7tsk3ktj5saheau", "7sxun4j01340isag",
                "rbawe8w8q58qhefi", "5r4vmr8pcxe0h3c6", "fbzfzmtntpx27rks", "fhwl1h4v02ossuuq", "cpnff8yerhfnvjnv", "ard704msen2vnmu7", "lda9g9hp53n0pfcp", "zd2l85gjavq8p5z9",
                "144usv3ex6u9mku7", "qargooeziv5f7qzj", "e6s4c5hfkwafmc5f", "wyga7a3umvmv03ki", "oz6l1o9xipganuzp", "f7nie5vzf5q0bdex", "9516fmiti2oaxp9g", "j30f9r8sd6x0ghw9",
                "quqr3ucm0wg7ib57", "1a9pciigfczwje7c", "7b40efpuqxvtxkgj", "otjw4w1c99uzy0mc", "haeiej7ytxeeeq59", "pdk7aewt4cvuxhbo", "kvtrmdajrv3t0dxd", "f9suu2tflcmk2szc",
                "t95a9iatpow92a9k", "0u4q08l6fv2fmf50", "xe6h1k893u7dh40w", "p20q96z7aydg4xlh", "lcizxz7q0ipqrjro", "xa019ycqe4dt8jqf", "5nv5n6a6bjkavae6", "pbua7pmqumuqgvov",
                "lwwyejy1hjvn0ueb", "b2kpsybo0xjk9e7x", "4axzxv1nm1zghmio", "4hul3jplvta3l1ou", "vxnk780asahqntri", "n688ooh738qu7h0g", "l52k8wwew2zen7v1", "uvz92nr37cdje7ib",
                "sknyd1h7exilqhnv", "78o4u5p6vp4zuvr7", "97bw70ya9ikwwq5f", "7fs5bu1zvhojozqo", "8d1716vyaxhv43xk", "oa3hi504rjz2wgzu", "yydyq2ghqgai3bfx", "lmousbqqyle960pw",
                "s7tbmh01muv14ktw", "q8iekladqooqdq58", "0h6p17dcv4cdocux", "i9yqc2eusd7xd4hr", "dgfa1m5zgg0jrhtf", "eewl1ob138l6vrow", "e5vp624ypkwh0r03", "l7l9t83ribvvfyqz",
                "3rclgjgv64bigwej", "uk6vm3tahza7anwb", "xq7nffvtq9iq9woo", "gnfwrab6y2lhcewx", "q2bi6fnia566wc27", "bcdye9vujz7futuh", "iptk5asnjiwssomb", "9gzcubbvieeu1i14",
                "ajnmurqcl5hrvkv8", "qlt140ar6iyom6um", "goc7vjvvqddwk20l", "5jctduujw2d67toz", "ruamtrz02uytxkbm", "8auoji5ajf1nyhmb", "3j18sxkpbnpzt8jb", "1wrgq8dgl8dfm7cr",
                "4qg5cazsfj28hzqq", "bs2kwwg7ws24sr4i", "exyjx8p1bs6ccr2n", "i3xzww1tq50ohn1u", "2b31yow56wwc4pv4", "q4fgxhxv5o74el35", "m3o3i8ht54y2sgfj", "jmuyeiv0l4e37yx2",
                "nzwwu1c9zk9la17h", "w6ni9cskxmikgvs4", "rus2vw3rz4rjyqls", "65wjlj2qu8q6asun", "ha56l9xudxav78c9", "jmcoiksdrh2q4cnc", "qs4j4oqs7t7euc6c", "w7waqu2v2ovbrvvm",
                "6yylrjn85p0iuheo", "4s5426bc05bl7c32", "gy9p5qn404rz4mq0", "i8jld1qs8qgygizi", "0l7ocsosuhvcn1ia", "ia4f428ais2pabb2", "9yndfjpmr9qct0na", "75hd8j56c0k7kz86",
                "iirvvdevdf968lz5", "8js4t5cfdkab4734", "tgrz8o80qbzzsrim", "2t7uht5mnbelmovp", "ixi1tsgdc09p0zt8", "ca65265wuhlf53l3", "do689esjcmg5m5hd", "yosk07bzsgo8zv9w",
                "r2921m40nvfiirge", "c7z4ejnpsitgoavj", "tvoaudu9lhlz3fyr", "1su4ahqem55bea75", "e7g8tv4wqxwgxjb9", "2qmtr0aw77oppmq9", "oesj2ealrk8kn0nk", "f11p4b8dxycnqyq9",
                "q96sg3dgkgkl17k8", "koryhlaehyejq90i", "aklmcpwyyp8cwfdf", "p43d0bc40ur9moaq", "hdbkm0c7eeh8jj3r", "r7rz5w0w4caveaxx", "bv5c6ba44f216lus", "vaj8cfbqz189zdni",
                "dcy0lbdq4n78jzmv", "e7v0ilb7blz9izxb", "08h05rhjfgpm16gv", "1j4gkda3afd7n5av", "dz16elxv8xrgujc2", "q3zydhp0ey8evnxa", "j597n8qifdgkk81q", "ge7nyucuodc5acx1",
                "32tcbnzpca5jzt9w", "4oyrau2wxfl6rrxp", "nnamieq5nn0hofnp", "khp76hvkl692pxs9", "h8db7xrab4u6p87e", "murg2fna0ous8uxq", "o93z6zuk7itqjeig", "x8u8gw6g4z97u51n",
                "m0cidfdpiiw9sje9", "g7p9bz76c2ejqu3p", "6oiykh9fu736ovbk", "sh3jfzsj6uqhik8c", "hvsvjq9zkprb97y8", "f0ipqj8qv86ady4e", "7icxavgnem2oklwr", "h6d7t52sarayaelu",
                "w30p4dloq4eusvh7", "i9jvbkferzl3as0v", "6xp5jyz9n3h5agf6", "q06496zizs333ajg", "i4w9y9t0lrr1yn9f", "7er1w2yurm57plbj", "1wj5pulke7xvc6rq", "1ftcmq4xhgmz4zmd",
                "1sc2rrqwl88s7jvp", "w57ptbcf9foj6m24", "ym0s2o1gn8frrrfm", "ryy2kyag7c7irtmf", "ntkotv5t9pxmo20r", "yra7lkuanbuhho9c", "avw95rphcs85w4j9", "n04zw9wy6wtnbcs6",
                "m9xj5d6nb1bfpou8", "jltm2vtk7wm4krdp", "c97wxgqn3utj7y01"
    };

    public static int countUnlockedSkills(Identifier categoryId, ServerPlayerEntity player) {
        if (!ASCENDANCY.equals(categoryId)) return 0;
        int count = 0;
        for (String id : ASCENDANCY_SKILLS) if (isSkillUnlocked(categoryId,id,player)) count++;
        return count;
    }

    /** Continued parity: Ascendancy becomes visible/unlocked after more than 40
     * unlocked skills in the base tree category. */
    public static void ensureAscendancyUnlocked(ServerPlayerEntity player) {
        Optional<Category> tree=category(TREE);
        Optional<Category> asc=category(ASCENDANCY);
        if(tree.isEmpty() || asc.isEmpty() || asc.get().isUnlocked(player)) return;
        if(tree.get().streamUnlockedSkills(player).count()>40) asc.get().unlock(player);
    }

    public static Optional<Category> selectedClass(ServerPlayerEntity player) {
        List<Category> unlocked = CLASS_CATEGORIES.stream()
                .map(SkillsAPI::getCategory).flatMap(Optional::stream)
                .filter(c -> c.isUnlocked(player)).toList();
        return unlocked.size() == 1 ? Optional.of(unlocked.get(0)) : Optional.empty();
    }
}
