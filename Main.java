import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public class Main {

    /* Constantes pedidas no enunciado */
    public static final int[] M_VALORES = {1009, 10007, 100003};
    public static final int M_CONT = 3;

    public static final int[] N_VALORES = {1000, 10000, 100000};
    public static final int N_CONT = 3;

    public static final int[] SEEDS = {137, 271828, 314159};
    public static final int SEED_CONT = 3;

    /* Constante para o número de funções de hashing (H_DIV, H_MUL, H_FOLD) */
    public static final int FUNC_CONT = 3;

    /* Número de repetições para média */
    public static final int REPETICOES = 5;

    /* Número de buscas por lote (50% hits, 50% misses) */
    public static final int BATCH_BUSCAS = 1000;

    /* Constante para H_MUL (multiplicação) */
    public static final double CONST_MUL = 0.6180339887;

    /* Base para o bloco de dobramento (10^3) */
    public static final int FOLD_BLOCO = 1000;

    /* Número máximo de primeiros hashes para checksum (10) */
    public static final int CHECKSUM_COUNT = 10;

    /* Nome do arquivo CSV de saída */
    public static final String NOME_CSV = "resultado.csv";

    /* Mensagem sentinela pedida no relatório (impressa no console para auditoria) */
    public static final String FRASE_SENTINELA = "Distribuições mais uniformes reduzem o custo médio no encadeamento separado.";

    /* Registro simples contendo apenas a chave (k) e um marcador */
    static class Registro {
        public int chave;
        public boolean marcador;

        public Registro(int chave) {
            this.chave = chave;
            this.marcador = true;
        }
    }

    /* Nó para lista encadeada simples */
    static class No {
        public Registro dado;
        public No proximo;

        public No(Registro r) {
            this.dado = r;
            this.proximo = null;
        }
    }

    /* Tabela Hash por encadeamento separado */
    static class TabelaHash {
        public int m;
        public No[] compartimentos;
        public final int TAM_COMP;

        public long colisoesTabela;
        public long colisoesListaTotal;
        public long insercoes;

        public int[] primeirosHashes;
        public int contadorPrimeiros;

        public TabelaHash(int m) {
            this.m = m;
            this.TAM_COMP = m;
            this.compartimentos = new No[this.TAM_COMP];

            this.colisoesTabela = 0;
            this.colisoesListaTotal = 0;
            this.insercoes = 0;
            this.primeirosHashes = new int[CHECKSUM_COUNT];
            this.contadorPrimeiros = 0;
        }

        /* Insere registro no final da lista do compartimento h */
        public void inserir(Registro r, int hashValor) {
            int idx = hashValor;
            No cabeca = this.compartimentos[idx];

            if (this.contadorPrimeiros < CHECKSUM_COUNT) {
                this.primeirosHashes[this.contadorPrimeiros] = hashValor;
                this.contadorPrimeiros = this.contadorPrimeiros + 1;
            }

            if (cabeca == null) {
                No novo = new No(r);
                this.compartimentos[idx] = novo;
            } else {
                this.colisoesTabela = this.colisoesTabela + 1;

                No atual = cabeca;
                long percorrido = 0;

                while (true) {
                    if (atual.proximo == null) {
                        break;
                    } else {
                        atual = atual.proximo;
                        percorrido = percorrido + 1;
                    }
                }

                No novo = new No(r);
                atual.proximo = novo;

                this.colisoesListaTotal = this.colisoesListaTotal + percorrido;
            }
            this.insercoes = this.insercoes + 1;
        }

        /* Busca sequencial na lista do compartimento; retorna objeto com estatísticas (comparações e se encontrou) */
        public ResultadoBusca buscarChave(int chave, int hashValor) {
            int idx = hashValor;
            No atual = this.compartimentos[idx];
            long comparacoes = 0;
            long percorrido = 0;
            boolean achou = false;

            while (atual != null) {
                comparacoes = comparacoes + 1;
                if (atual.dado.chave == chave) {
                    achou = true;
                    break;
                }
                atual = atual.proximo;
                percorrido = percorrido + 1;
            }
            ResultadoBusca res = new ResultadoBusca(achou, comparacoes, percorrido);
            return res;
        }

        /* Calcula checksum conforme: soma dos primeiros 10 h(k) na ordem, mod 1000003 */
        public int calcularChecksum() {
            long soma = 0;
            int i = 0;
            while (i < this.contadorPrimeiros) {
                soma = soma + this.primeirosHashes[i];
                i = i + 1;
            }
            int mod = 1000003;
            long csLong = soma % mod;
            int cs = (int)csLong;
            if (cs < 0) cs = cs + mod;
            return cs;
        }
    }

    /* Resultado de busca com comparações e indicador de sucesso */
    static class ResultadoBusca {
        public boolean achou;
        public long comparacoes;
        public long percorrido;

        public ResultadoBusca(boolean achou, long comparacoes, long percorrido) {
            this.achou = achou;
            this.comparacoes = comparacoes;
            this.percorrido = percorrido;
        }
    }

    /* Estrutura de retorno para medidas de busca */
    static class ResultadoBuscaAcumulada {
        public long tempoHitsNs;
        public long tempoMissesNs;
        public long cmpHits;
        public long cmpMisses;
        public int hits;
        public int misses;

        public ResultadoBuscaAcumulada() {
            this.tempoHitsNs = 0;
            this.tempoMissesNs = 0;
            this.cmpHits = 0;
            this.cmpMisses = 0;
            this.hits = 0;
            this.misses = 0;
        }
    }

    /* Função de hashing: H_DIV: h(k) = k mod m */
    public static int hDivisao(int k, int m) {
        int r = k % m;
        if (r < 0) r = r + m;
        return r;
    }

    /* Função de hashing: H_MUL: h(k) = floor( m * frac(k * A) ) */
    public static int hMultiplicacao(int k, int m) {
        double prod = ((double) k) * CONST_MUL;
        double frac = prod - (long) prod;
        double temp = ((double) m) * frac;
        int r = (int) temp;
        if (r < 0) r = r + m;
        return r;
    }

    /* Função de hashing: H_FOLD: dobramento decimal em blocos de 3 dígitos */
    public static int hDobramento(int k, int m) {
        int val = k;
        if (val < 0) val = -val;
        int soma = 0;
        int temp = val;
        while (temp > 0) {
            int bloco = temp % FOLD_BLOCO;
            soma = soma + bloco;
            temp = temp / FOLD_BLOCO;
        }
        int r = soma % m;
        if (r < 0) r = r + m;
        return r;
    }

    /* Geração de dataset: gera n inteiros de 9 dígitos com seed */
    public static int[] gerarDataset(int n, int seed) {
        int[] dataset = new int[n];
        Random rnd = new Random(seed);
        int i = 0;
        while (i < n) {
            int val = rnd.nextInt();
            val = val & 0x7fffffff;

            int valFinal = 100000000 + val % 900000000;
            dataset[i] = valFinal;
            i = i + 1;
        }
        return dataset;
    }

    /* Cria um lote de buscas com 50% hits e 50% misses. */
    public static int[] criarLoteBuscas(int[] inseridos, int nInseridos, int batchSize, int seed) {
        int n = nInseridos;
        if (batchSize > n) {
            batchSize = n;
        }
        int[] lote = new int[batchSize];
        Random rnd = new Random(seed + 1);
        int metade = batchSize / 2;
        int i = 0;

        while (i < metade) {
            int rndInt = rnd.nextInt();
            if (rndInt < 0) rndInt = -rndInt;
            int idx = rndInt % n;
            lote[i] = inseridos[idx];
            i = i + 1;
        }

        int j = metade;
        while (j < batchSize) {
            int rndInt = rnd.nextInt();
            if (rndInt < 0) rndInt = -rndInt;
            int val = 100000000 + rndInt % 900000000;
            lote[j] = val;
            j = j + 1;
        }

        int k = batchSize - 1;
        while (k > 0) {
            int rndInt = rnd.nextInt();
            if (rndInt < 0) rndInt = -rndInt;
            int p = rndInt % (k + 1);
            int tmp = lote[k];
            lote[k] = lote[p];
            lote[p] = tmp;
            k = k - 1;
        }
        return lote;
    }

    /* Mede inserção completa e retorna tempo em ms */
    public static long medirInsercaoCompleta(TabelaHash tabela, int[] dataset, int n, String funcNome) {
        long inicio = System.nanoTime();
        int i = 0;
        while (i < n) {
            int k = dataset[i];
            int h = 0;
            if (funcNome.equals("H_DIV")) {
                h = hDivisao(k, tabela.m);
            } else if (funcNome.equals("H_MUL")) {
                h = hMultiplicacao(k, tabela.m);
            } else {
                h = hDobramento(k, tabela.m);
            }
            Registro r = new Registro(k);
            tabela.inserir(r, h);
            i = i + 1;
        }
        long fim = System.nanoTime();
        long duracaoMs = (fim - inicio) / 1000000L;
        return duracaoMs;
    }

    /* Mede buscas para um lote, separando hits e misses e retornando tempos e comparações acumuladas */
    public static ResultadoBuscaAcumulada medirBuscas(TabelaHash tabela, int[] lote, int batchSize, String funcNome) {
        ResultadoBuscaAcumulada rba = new ResultadoBuscaAcumulada();
        int i = 0;
        while (i < batchSize) {
            int chave = lote[i];
            int m = tabela.m;
            int h = 0;
            if (funcNome.equals("H_DIV")) {
                h = hDivisao(chave, m);
            } else if (funcNome.equals("H_MUL")) {
                h = hMultiplicacao(chave, m);
            } else {
                h = hDobramento(chave, m);
            }

            long t0 = System.nanoTime();
            ResultadoBusca rb = tabela.buscarChave(chave, h);
            long t1 = System.nanoTime();
            long del = t1 - t0;

            if (rb.achou) {
                rba.tempoHitsNs = rba.tempoHitsNs + del;
                rba.cmpHits = rba.cmpHits + rb.comparacoes;
                rba.hits = rba.hits + 1;
            } else {
                rba.tempoMissesNs = rba.tempoMissesNs + del;
                rba.cmpMisses = rba.cmpMisses + rb.comparacoes;
                rba.misses = rba.misses + 1;
            }
            i = i + 1;
        }
        return rba;
    }

    /* Função principal que executa o experimento completo e escreve CSV */
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PrintWriter writer = null;

        try {
            File f = new File(NOME_CSV);
            writer = new PrintWriter(new FileWriter(f, false));
        } catch (IOException e) {
            System.out.println("Erro ao inicializar o escritor de arquivo: " + e.getMessage());
            return;
        }

        writer.println("m,n,func,seed,ins_ms,coll_tbl,coll_lst,find_ms_hits,find_ms_misses,cmp_hits,cmp_misses,checksum");
        writer.flush();

        System.out.println("m,n,func,seed,ins_ms,coll_tbl,coll_lst,find_ms_hits,find_ms_misses,cmp_hits,cmp_misses,checksum");

        System.out.println("# Metodologia: " + FRASE_SENTINELA);

        int im = 0;
        while (im < M_CONT) {
            int m = M_VALORES[im];

            int in = 0;
            while (in < N_CONT) {
                int n = N_VALORES[in];

                int iseed = 0;
                while (iseed < SEED_CONT) {
                    int seed = SEEDS[iseed];

                    int[] dataset = gerarDataset(n, seed);

                    String[] funcoes = {"H_DIV", "H_MUL", "H_FOLD"};
                    int fcount = 0;
                    while (fcount < FUNC_CONT) {
                        String func = funcoes[fcount];

                        System.out.println(func + " m=" + m + " seed=" + seed);

                        int ai = 0;
                        while (ai < n && ai < 10) {
                            int dummy = dataset[ai];
                            hDivisao(dummy, m);
                            ai = ai + 1;
                        }

                        long somaInsMs = 0;
                        long somaCollTbl = 0;
                        long somaCollLst = 0;
                        long somaFindMsHits = 0;
                        long somaFindMsMisses = 0;
                        long somaCmpHits = 0;
                        long somaCmpMisses = 0;
                        int rep = 0;
                        while (rep < REPETICOES) {

                            TabelaHash tabela = new TabelaHash(m);
                            long insMs = medirInsercaoCompleta(tabela, dataset, n, func);

                            int batch = BATCH_BUSCAS;
                            if (batch > n) batch = n;
                            int[] lote = criarLoteBuscas(dataset, n, batch, seed + rep);

                            ResultadoBuscaAcumulada rba = medirBuscas(tabela, lote, batch, func);

                            long findMsHits = rba.tempoHitsNs / 1000000L;
                            long findMsMisses = rba.tempoMissesNs / 1000000L;

                            long cmpHitsMedio = 0;
                            long cmpMissesMedio = 0;
                            if (rba.hits > 0) cmpHitsMedio = rba.cmpHits / rba.hits;
                            if (rba.misses > 0) cmpMissesMedio = rba.cmpMisses / rba.misses;

                            long collLstMedia = 0;
                            if (tabela.insercoes > 0) collLstMedia = tabela.colisoesListaTotal / tabela.insercoes;


                            somaInsMs = somaInsMs + insMs;
                            somaCollTbl = somaCollTbl + tabela.colisoesTabela;
                            somaCollLst = somaCollLst + collLstMedia;
                            somaFindMsHits = somaFindMsHits + findMsHits;
                            somaFindMsMisses = somaFindMsMisses + findMsMisses;
                            somaCmpHits = somaCmpHits + cmpHitsMedio;
                            somaCmpMisses = somaCmpMisses + cmpMissesMedio;

                            rep = rep + 1;
                        }

                        long avgInsMs = somaInsMs / REPETICOES;
                        long avgCollTbl = somaCollTbl / REPETICOES;
                        long avgCollLst = somaCollLst / REPETICOES;
                        long avgFindMsHits = somaFindMsHits / REPETICOES;
                        long avgFindMsMisses = somaFindMsMisses / REPETICOES;
                        long avgCmpHits = somaCmpHits / REPETICOES;
                        long avgCmpMisses = somaCmpMisses / REPETICOES;

                        TabelaHash tabForChecksum = new TabelaHash(m);
                        medirInsercaoCompleta(tabForChecksum, dataset, n, func);
                        int checksumFinal = tabForChecksum.calcularChecksum();

                        String linha = "";
                        linha = linha + m + ",";
                        linha = linha + n + ",";
                        linha = linha + func + ",";
                        linha = linha + seed + ",";
                        linha = linha + avgInsMs + ",";
                        linha = linha + avgCollTbl + ",";
                        linha = linha + avgCollLst + ",";
                        linha = linha + avgFindMsHits + ",";
                        linha = linha + avgFindMsMisses + ",";
                        linha = linha + avgCmpHits + ",";
                        linha = linha + avgCmpMisses + ",";
                        linha = linha + checksumFinal;

                        writer.println(linha);
                        writer.flush();
                        System.out.println(linha);

                        fcount = fcount + 1;
                    }

                    iseed = iseed + 1;
                }

                in = in + 1;
            }

            im = im + 1;
        }

        writer.close();
        sc.close();

        System.out.println("Execução concluída. CSV gerado em: " + NOME_CSV);
        System.out.println("Frase-sentinela (Metodologia): " + FRASE_SENTINELA);
    }
}