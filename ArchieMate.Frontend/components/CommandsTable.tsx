import React from "react";
import Paper from "@mui/material/Paper";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TablePagination from "@mui/material/TablePagination";
import TableRow from "@mui/material/TableRow";
import CircularProgress from "@mui/material/CircularProgress";
import TablePaginationActions from "./TablePaginationActions";

interface Column {
  id: "name" | "response";
  label: string;
  minWidth?: number;
  align?: "right";
  format?: (value: number) => string;
}

const columns: Column[] = [
  { id: "name", label: "Command", minWidth: 70 },
  { id: "response", label: "Response", minWidth: 170 },
];

interface Data {
  name: string;
  response: string;
}

interface ApiData extends Data {
  Id: string;
}

export interface ICommandsTableProps {
  channelName: string;
}

const CommandsTable = (props: ICommandsTableProps) => {
  const [page, setPage] = React.useState(0);
  const [rowsPerPage, setRowsPerPage] = React.useState(10);

  const handleChangePage = (event: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    setRowsPerPage(+event.target.value);
    setPage(0);
  };

  const [rows, setRows] = React.useState<Data[]>();

  React.useEffect(() => {
    const fetchData = async (): Promise<Data[]> => {
      const response = await fetch(
        `/api/commands/channel/${props.channelName}`
      );
      let finalData: Data[] = [];
      if (response.ok) {
        const json: ApiData[] = await response.json();
        finalData = json.map((data) => {
          return { name: `!${data.name}`, response: data.response };
        });
      } else {
        finalData = [
          {
            name: "Error",
            response: `Couldn't fetch the data (status: ${response.status} | statusText: ${response.statusText})`,
          },
        ];
      }
      return finalData;
    };

    fetchData().then((data: Data[]) => {
      setRows(data);
    });
  }, [setRows, props.channelName]);

  return (
    <Paper sx={{ width: "100%", overflow: "hidden" }}>
      <TableContainer sx={{ maxHeight: 440 }}>
        <Table stickyHeader aria-label="sticky table">
          <TableHead>
            <TableRow>
              {columns.map((column) => (
                <TableCell
                  key={column.id}
                  align={column.align}
                  style={{ minWidth: column.minWidth }}
                >
                  {column.label}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {rows === undefined && (
              <TableRow>
                <TableCell align="center" colSpan={2}>
                  <CircularProgress />
                </TableCell>
              </TableRow>
            )}
            {rows &&
              (rowsPerPage > 0
                ? rows.slice(
                    page * rowsPerPage,
                    page * rowsPerPage + rowsPerPage
                  )
                : rows
              ).map((row) => {
                return (
                  <TableRow hover role="checkbox" tabIndex={-1} key={row.name}>
                    {columns.map((column) => {
                      const value = row[column.id];
                      return (
                        <TableCell key={column.id} align={column.align}>
                          {column.format && typeof value === "number"
                            ? column.format(value)
                            : value}
                        </TableCell>
                      );
                    })}
                  </TableRow>
                );
              })}
          </TableBody>
        </Table>
      </TableContainer>
      <TablePagination
        labelRowsPerPage="Per page:"
        rowsPerPageOptions={[10, 20, 50, { value: -1, label: "All" }]}
        colSpan={2}
        component="div"
        count={rows ? rows.length : 0}
        rowsPerPage={rowsPerPage}
        page={page}
        onPageChange={handleChangePage}
        onRowsPerPageChange={handleChangeRowsPerPage}
        ActionsComponent={TablePaginationActions}
      />
    </Paper>
  );
};

export default CommandsTable;
