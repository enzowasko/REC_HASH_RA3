# README — Tabela Hash por Encadeamento Separado

Aluno: Enzo Wasko Amorim

## Descrição

Implementação em Java de uma Tabela Hash por **encadeamento separado** com medições experimentais. O programa executa a geração de datasets reprodutíveis, insere chaves na tabela usando três funções de hashing (H_DIV, H_MUL, H_FOLD), mede tempos e colisões, e grava os resultados em CSV.

## Itens implementados

* Tamanhos de tabela (m): `1009`, `10007`, `100003` (`M_VALORES`).
* Tamanhos de datasets (n): `1000`, `10000`, `100000` (`N_VALORES`).
* Seeds: `137`, `271828`, `314159` (`SEEDS`).
* Funções de hashing:

  * `H_DIV`: h(k) = k mod m.
  * `H_MUL`: h(k) = floor(m * frac(k * A)) com `A = 0.6180339887` (`CONST_MUL`).
  * `H_FOLD`: dobramento decimal em blocos de 3 dígitos (`FOLD_BLOCO = 1000`).
* Repetições para média: `REPETICOES = 5`.
* Lote de buscas: `BATCH_BUSCAS = 1000` (50% hits, 50% misses).
* Checksum: soma dos primeiros 10 valores `h(k)` (ordem de inserção) mod `1000003` (`CHECKSUM_COUNT = 10`).
* Sentinela (impressa no console):
  `Distribuições mais uniformes reduzem o custo médio no encadeamento separado.` (`FRASE_SENTINELA`).
* Estruturas internas:

  * `Registro` (chave e marcador).
  * `No` (nó da lista encadeada).
  * `TabelaHash` (array de compartimentos e estatísticas).
  * `ResultadoBusca` / `ResultadoBuscaAcumulada` para medições de busca.

## Saída CSV

* Nome do arquivo: `resultado.csv` (`NOME_CSV`).
* Cabeçalho (exatamente, em minúsculas e nesta ordem):
  `m,n,func,seed,ins_ms,coll_tbl,coll_lst,find_ms_hits,find_ms_misses,cmp_hits,cmp_misses,checksum`
* O programa escreve o CSV no arquivo `resultado.csv` e também imprime as mesmas linhas no console.

## Observações de execução

* A `main` inicializa o escritor de arquivo (`PrintWriter`), escreve o cabeçalho CSV e executa todos os experimentos para combinações de `m`, `n`, `seed` e cada função de hashing.
* Antes de cada experimento o programa imprime no console a etiqueta de auditoria com a função, `m` e `seed`, por exemplo: `H_DIV m=1009 seed=137`.
* Após a conclusão, o programa imprime:

  * `Execução concluída. CSV gerado em: resultado.csv`
  * `Frase-sentinela (Metodologia): Distribuições mais uniformes reduzem o custo médio no encadeamento separado.`

## Observações técnicas

* Geração dos números de 9 dígitos com `Random(seed)`.
* Medição de tempo usando `System.nanoTime()` e conversão para milissegundos.
* Tratamento de escrita em arquivo com `FileWriter` / `PrintWriter` e captura de `IOException` ao inicializar o escritor.
* Todas as constantes e nomes de variáveis estão em português, conforme o código.

## Execução

* Execute a classe `Main` (ex.: rodando `Main.java`). O programa gerará `resultado.csv` no diretório de execução e imprimirá saída no console.
